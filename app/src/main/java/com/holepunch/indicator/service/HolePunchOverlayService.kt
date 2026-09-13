package com.holepunch.indicator.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.holepunch.indicator.model.IndicatorState
import com.holepunch.indicator.ui.MainActivity
import com.holepunch.indicator.view.PunchHoleOverlayView

class HolePunchOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: PunchHoleOverlayView? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentState = IndicatorState()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 50
                mainHandler.post {
                    currentState = currentState.copy(
                        batteryPercent = pct,
                        isCharging = isCharging
                    )
                    overlayView?.updateState(currentState)
                }
            }
        }
    }

    private val audioReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.RINGER_MODE_CHANGED_ACTION) {
                updateRingerState()
            }
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED,
                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    updateBluetoothState()
                }
            }
        }
    }

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        startForegroundNotification()
        setupOverlay()
        registerMonitors()
    }

    private fun startForegroundNotification() {
        val channelId = "hole_punch_indicator_channel"
        val channelName = "HolePunch Indicator Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps camera hole punch status indicator active"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("PunchHole Indicator")
            .setContentText("Status indicators are active around camera")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    @SuppressLint("RtlHardcoded")
    private fun setupOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutParams = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.LEFT
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }

        overlayView = PunchHoleOverlayView(this)
        try {
            windowManager?.addView(overlayView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun registerMonitors() {
        // Battery
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        ContextCompat.registerReceiver(
            this,
            batteryReceiver,
            batteryFilter,
            ContextCompat.RECEIVER_EXPORTED
        )

        // Ringer / Silent
        val audioFilter = IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
        ContextCompat.registerReceiver(
            this,
            audioReceiver,
            audioFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
        updateRingerState()

        // Bluetooth
        val btFilter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        ContextCompat.registerReceiver(
            this,
            bluetoothReceiver,
            btFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
        updateBluetoothState()

        // Network (Wi-Fi & Cellular)
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                val hasWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                val hasCellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)

                mainHandler.post {
                    currentState = currentState.copy(
                        isWifiConnected = hasWifi,
                        isCellularConnected = hasCellular
                    )
                    overlayView?.updateState(currentState)
                }
            }

            override fun onLost(network: Network) {
                mainHandler.post {
                    checkNetworkFallback()
                }
            }
        }

        try {
            connectivityManager?.registerDefaultNetworkCallback(networkCallback!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkNetworkFallback() {
        val cm = connectivityManager ?: return
        val activeNetwork = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(activeNetwork)
        val hasWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val hasCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        currentState = currentState.copy(
            isWifiConnected = hasWifi,
            isCellularConnected = hasCellular
        )
        overlayView?.updateState(currentState)
    }

    private fun updateRingerState() {
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val isSilentOrVibrate = am?.ringerMode != AudioManager.RINGER_MODE_NORMAL
            mainHandler.post {
                currentState = currentState.copy(isSilentOrVibrate = isSilentOrVibrate)
                overlayView?.updateState(currentState)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateBluetoothState() {
        try {
            val hasBtPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            } else true

            val bm = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bm?.adapter
            val isConnected = if (hasBtPerm) adapter?.isEnabled == true else false

            mainHandler.post {
                currentState = currentState.copy(isBluetoothConnected = isConnected)
                overlayView?.updateState(currentState)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun notifySettingsChanged() {
        mainHandler.post {
            overlayView?.notifySettingsChanged()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null

        try {
            unregisterReceiver(batteryReceiver)
            unregisterReceiver(audioReceiver)
            unregisterReceiver(bluetoothReceiver)
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (overlayView != null && windowManager != null) {
            try {
                if (overlayView?.isAttachedToWindow == true) {
                    windowManager?.removeView(overlayView)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        var instance: HolePunchOverlayService? = null
    }
}