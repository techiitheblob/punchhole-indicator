package com.holepunch.indicator.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SERVICE_ENABLED, value).apply()

    var offsetX: Float
        get() = prefs.getFloat(KEY_OFFSET_X, 0f)
        set(value) = prefs.edit().putFloat(KEY_OFFSET_X, value).apply()

    var offsetY: Float
        get() = prefs.getFloat(KEY_OFFSET_Y, 0f)
        set(value) = prefs.edit().putFloat(KEY_OFFSET_Y, value).apply()

    var cutoutRadiusDp: Float
        get() = prefs.getFloat(KEY_CUTOUT_RADIUS_DP, 18f)
        set(value) = prefs.edit().putFloat(KEY_CUTOUT_RADIUS_DP, value).apply()

    var ringThicknessDp: Float
        get() = prefs.getFloat(KEY_RING_THICKNESS_DP, 3.5f)
        set(value) = prefs.edit().putFloat(KEY_RING_THICKNESS_DP, value).apply()

    var ringGapDp: Float
        get() = prefs.getFloat(KEY_RING_GAP_DP, 3.0f)
        set(value) = prefs.edit().putFloat(KEY_RING_GAP_DP, value).apply()

    var dotRadiusDp: Float
        get() = prefs.getFloat(KEY_DOT_RADIUS_DP, 3.5f)
        set(value) = prefs.edit().putFloat(KEY_DOT_RADIUS_DP, value).apply()

    var dotDistanceDp: Float
        get() = prefs.getFloat(KEY_DOT_DISTANCE_DP, 27f)
        set(value) = prefs.edit().putFloat(KEY_DOT_DISTANCE_DP, value).apply()

    var dotSpreadAngle: Float
        get() = prefs.getFloat(KEY_DOT_SPREAD_ANGLE, 80f)
        set(value) = prefs.edit().putFloat(KEY_DOT_SPREAD_ANGLE, value).apply()

    var isTestMode: Boolean
        get() = prefs.getBoolean(KEY_TEST_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_TEST_MODE, value).apply()

    fun resetToDefaults() {
        prefs.edit()
            .putFloat(KEY_OFFSET_X, 0f)
            .putFloat(KEY_OFFSET_Y, 0f)
            .putFloat(KEY_CUTOUT_RADIUS_DP, 18f)
            .putFloat(KEY_RING_THICKNESS_DP, 3.5f)
            .putFloat(KEY_RING_GAP_DP, 3.0f)
            .putFloat(KEY_DOT_RADIUS_DP, 3.5f)
            .putFloat(KEY_DOT_DISTANCE_DP, 27f)
            .putFloat(KEY_DOT_SPREAD_ANGLE, 80f)
            .putBoolean(KEY_TEST_MODE, false)
            .apply()
    }

    companion object {
        private const val PREF_NAME = "punchhole_prefs"
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        private const val KEY_OFFSET_X = "offset_x"
        private const val KEY_OFFSET_Y = "offset_y"
        private const val KEY_CUTOUT_RADIUS_DP = "cutout_radius_dp"
        private const val KEY_RING_THICKNESS_DP = "ring_thickness_dp"
        private const val KEY_RING_GAP_DP = "ring_gap_dp"
        private const val KEY_DOT_RADIUS_DP = "dot_radius_dp"
        private const val KEY_DOT_DISTANCE_DP = "dot_distance_dp"
        private const val KEY_DOT_SPREAD_ANGLE = "dot_spread_angle"
        private const val KEY_TEST_MODE = "test_mode"
    }
}
