package com.holepunch.indicator.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.holepunch.indicator.data.PreferencesManager
import com.holepunch.indicator.databinding.ActivityMainBinding
import com.holepunch.indicator.service.HolePunchOverlayService

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)

        setupPermissionButtons()
        setupCalibrationSliders()
        setupMasterSwitch()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun setupPermissionButtons() {
        binding.btnOverlayPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:")
                )
                startActivity(intent)
            }
        }

        binding.btnBatteryOpt.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:")
                )
                startActivity(intent)
            }
        }

        binding.btnNotificationPerm.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQ_NOTIF
                )
            } else {
                Toast.makeText(this, "Notification permission granted by default", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePermissionStatus() {
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true

        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isBatteryIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm?.isIgnoringBatteryOptimizations(packageName) == true
        } else true

        binding.btnOverlayPermission.text = if (hasOverlay) {
            "Overlay Permission: GRANTED"
        } else {
            "Grant Overlay Permission"
        }
        binding.btnOverlayPermission.isEnabled = !hasOverlay

        binding.btnBatteryOpt.text = if (isBatteryIgnored) {
            "Battery Optimization: EXEMPTED"
        } else {
            "Disable Battery Optimization"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotif = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            binding.btnNotificationPerm.text = if (hasNotif) {
                "Notifications: GRANTED"
            } else {
                "Grant Notification Permission"
            }
        }
    }

    private fun setupMasterSwitch() {
        binding.switchMaster.isChecked = prefs.isServiceEnabled && HolePunchOverlayService.instance != null

        binding.switchMaster.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Please grant Overlay Permission first", Toast.LENGTH_LONG).show()
                    binding.switchMaster.isChecked = false
                    return@setOnCheckedChangeListener
                }
                prefs.isServiceEnabled = true
                startOverlayService()
            } else {
                prefs.isServiceEnabled = false
                stopOverlayService()
            }
        }
    }

    private fun startOverlayService() {
        val intent = Intent(this, HolePunchOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopOverlayService() {
        val intent = Intent(this, HolePunchOverlayService::class.java)
        stopService(intent)
    }

    private fun setupCalibrationSliders() {
        // Test mode switch
        binding.switchTestMode.isChecked = prefs.isTestMode
        binding.switchTestMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.isTestMode = isChecked
            HolePunchOverlayService.instance?.notifySettingsChanged()
        }

        // Slider X
        binding.sliderOffsetX.value = prefs.offsetX.coerceIn(-150f, 150f)
        binding.labelOffsetX.text = "Center X Offset:  px"
        binding.sliderOffsetX.addOnChangeListener { _, value, _ ->
            prefs.offsetX = value
            binding.labelOffsetX.text = "Center X Offset:  px"
            HolePunchOverlayService.instance?.notifySettingsChanged()
        }

        // Slider Y
        binding.sliderOffsetY.value = prefs.offsetY.coerceIn(-150f, 150f)
        binding.labelOffsetY.text = "Center Y Offset:  px"
        binding.sliderOffsetY.addOnChangeListener { _, value, _ ->
            prefs.offsetY = value
            binding.labelOffsetY.text = "Center Y Offset:  px"
            HolePunchOverlayService.instance?.notifySettingsChanged()
        }

        // Slider Cutout Radius
        binding.sliderRadius.value = prefs.cutoutRadiusDp.coerceIn(10f, 35f)
        binding.labelRadius.text = "Camera Cutout Radius:  dp"
        binding.sliderRadius.addOnChangeListener { _, value, _ ->
            prefs.cutoutRadiusDp = value
            binding.labelRadius.text = "Camera Cutout Radius:  dp"
            HolePunchOverlayService.instance?.notifySettingsChanged()
        }

        // Slider Ring Thickness
        binding.sliderThickness.value = prefs.ringThicknessDp.coerceIn(1.5f, 10f)
        binding.labelThickness.text = "Ring Thickness:  dp"
        binding.sliderThickness.addOnChangeListener { _, value, _ ->
            prefs.ringThicknessDp = value
            binding.labelThickness.text = "Ring Thickness:  dp"
            HolePunchOverlayService.instance?.notifySettingsChanged()
        }

        // Slider Ring Gap
        binding.sliderGap.value = prefs.ringGapDp.coerceIn(0f, 15f)
        binding.labelGap.text = "Ring Gap:  dp"
        binding.sliderGap.addOnChangeListener { _, value, _ ->
            prefs.ringGapDp = value
            binding.labelGap.text = "Ring Gap:  dp"
            HolePunchOverlayService.instance?.notifySettingsChanged()
        }

        // Slider Dot Distance
        binding.sliderDotDistance.value = prefs.dotDistanceDp.coerceIn(15f, 50f)
        binding.labelDotDistance.text = "Dot Distance:  dp"
        binding.sliderDotDistance.addOnChangeListener { _, value, _ ->
            prefs.dotDistanceDp = value
            binding.labelDotDistance.text = "Dot Distance:  dp"
            HolePunchOverlayService.instance?.notifySettingsChanged()
        }

        // Slider Dot Radius
        binding.sliderDotRadius.value = prefs.dotRadiusDp.coerceIn(1.5f, 7f)
        binding.labelDotRadius.text = "Dot Size:  dp"
        binding.sliderDotRadius.addOnChangeListener { _, value, _ ->
            prefs.dotRadiusDp = value
            binding.labelDotRadius.text = "Dot Size:  dp"
            HolePunchOverlayService.instance?.notifySettingsChanged()
        }

        // Reset to Defaults
        binding.btnResetDefaults.setOnClickListener {
            prefs.resetToDefaults()
            setupCalibrationSliders()
            HolePunchOverlayService.instance?.notifySettingsChanged()
            Toast.makeText(this, "Reset to default values", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val REQ_NOTIF = 101
    }
}
