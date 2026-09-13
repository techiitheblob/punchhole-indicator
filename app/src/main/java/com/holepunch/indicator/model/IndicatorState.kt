package com.holepunch.indicator.model

data class IndicatorState(
    val batteryPercent: Int = 85,
    val isCharging: Boolean = false,
    val isWifiConnected: Boolean = true,
    val isCellularConnected: Boolean = true,
    val isBluetoothConnected: Boolean = false,
    val isSilentOrVibrate: Boolean = false,
    val isTestMode: Boolean = false
)
