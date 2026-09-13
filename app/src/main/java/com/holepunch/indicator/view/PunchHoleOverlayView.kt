package com.holepunch.indicator.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.WindowInsets
import androidx.core.content.ContextCompat
import com.holepunch.indicator.R
import com.holepunch.indicator.data.PreferencesManager
import com.holepunch.indicator.model.IndicatorState
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class PunchHoleOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val prefs = PreferencesManager(context)
    private var state = IndicatorState()

    private var autoCenterX: Float = 0f
    private var autoCenterY: Float = 0f
    private var autoRadius: Float = dpToPx(18f)
    private var hasDetectedCutout: Boolean = false

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val batteryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1.5f)
        color = Color.MAGENTA
    }

    private val arcRect = RectF()

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    fun updateState(newState: IndicatorState) {
        this.state = newState
        postInvalidate()
    }

    fun notifySettingsChanged() {
        postInvalidate()
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val cutout = insets.displayCutout
            if (cutout != null && cutout.boundingRects.isNotEmpty()) {
                val rect = cutout.boundingRects.minByOrNull { it.top } ?: cutout.boundingRects[0]
                autoCenterX = rect.centerX().toFloat()
                autoCenterY = rect.centerY().toFloat()
                autoRadius = max(rect.width(), rect.height()) / 2f
                hasDetectedCutout = true
                postInvalidate()
            }
        }
        return super.onApplyWindowInsets(insets)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val fallbackX = if (width > 0) width / 2f else 540f
        val fallbackY = dpToPx(28f)

        val baseCenterX = if (hasDetectedCutout) autoCenterX else fallbackX
        val baseCenterY = if (hasDetectedCutout) autoCenterY else fallbackY

        val cx = baseCenterX + prefs.offsetX
        val cy = baseCenterY + prefs.offsetY

        val cutoutRadius = if (prefs.cutoutRadiusDp > 0) {
            dpToPx(prefs.cutoutRadiusDp)
        } else {
            autoRadius
        }

        val strokeWidth = dpToPx(prefs.ringThicknessDp)
        val gap = dpToPx(prefs.ringGapDp)
        val ringRadius = cutoutRadius + gap + (strokeWidth / 2f)

        // 1. Draw Optional Test / Calibration Guide
        if (prefs.isTestMode || state.isTestMode) {
            guidePaint.color = Color.parseColor("#E91E63")
            canvas.drawCircle(cx, cy, cutoutRadius, guidePaint)
            guidePaint.color = Color.parseColor("#00E5FF")
            canvas.drawLine(cx - cutoutRadius * 1.5f, cy, cx + cutoutRadius * 1.5f, cy, guidePaint)
            canvas.drawLine(cx, cy - cutoutRadius * 1.5f, cx, cy + cutoutRadius * 1.5f, guidePaint)
        }

        // 2. Battery Track & Progress Arc
        val startAngle = 135f
        val maxSweepAngle = 270f

        arcRect.set(cx - ringRadius, cy - ringRadius, cx + ringRadius, cy + ringRadius)

        // Background Track
        trackPaint.strokeWidth = strokeWidth
        trackPaint.color = ContextCompat.getColor(context, R.color.indicator_track)
        canvas.drawArc(arcRect, startAngle, maxSweepAngle, false, trackPaint)

        // Battery Active Arc
        val batteryPct = state.batteryPercent.coerceIn(0, 100)
        val batterySweep = (batteryPct / 100f) * maxSweepAngle

        batteryPaint.strokeWidth = strokeWidth
        batteryPaint.color = when {
            state.isCharging -> ContextCompat.getColor(context, R.color.indicator_battery_charging)
            batteryPct <= 15 -> ContextCompat.getColor(context, R.color.indicator_battery_low)
            else -> ContextCompat.getColor(context, R.color.indicator_battery_normal)
        }
        if (batterySweep > 0) {
            canvas.drawArc(arcRect, startAngle, batterySweep, false, batteryPaint)
        }

        // 3. Status Dots Along Lower Arc Contour
        val dotDistance = dpToPx(prefs.dotDistanceDp)
        val dotRadius = dpToPx(prefs.dotRadiusDp)
        val dotSpread = prefs.dotSpreadAngle

        val dotCount = 4
        val centerBottomAngle = 90.0
        val halfSpread = dotSpread / 2.0
        val angleStep = if (dotCount > 1) dotSpread / (dotCount - 1) else 0f

        val dotsData = listOf(
            Triple(R.color.dot_wifi_active, R.color.dot_wifi_inactive, state.isWifiConnected),
            Triple(R.color.dot_cellular_active, R.color.dot_cellular_inactive, state.isCellularConnected),
            Triple(R.color.dot_bluetooth_active, R.color.dot_bluetooth_inactive, state.isBluetoothConnected),
            Triple(R.color.dot_silent_active, R.color.dot_silent_inactive, state.isSilentOrVibrate)
        )

        for (i in 0 until dotCount) {
            val currentAngleDeg = (centerBottomAngle + halfSpread) - (i * angleStep)
            val rad = Math.toRadians(currentAngleDeg)

            val dotX = cx + (dotDistance * cos(rad)).toFloat()
            val dotY = cy + (dotDistance * sin(rad)).toFloat()

            val (activeColorRes, inactiveColorRes, isActive) = dotsData[i]
            dotPaint.color = ContextCompat.getColor(
                context,
                if (isActive) activeColorRes else inactiveColorRes
            )

            canvas.drawCircle(dotX, dotY, dotRadius, dotPaint)
        }
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        )
    }
}