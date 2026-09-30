package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BatteryState(
    val level: Int = 100, // 0 - 100
    val isCharging: Boolean = false,
    val chargePlug: String = "Battery", // AC, USB, Wireless, Battery
    val temperatureCelsius: Float = 25.0f,
    val isLowPower: Boolean = false, // level <= 20 and not charging
    val isCriticalPower: Boolean = false // level <= 10 and not charging
)

class BatteryMonitor(private val context: Context) {

    private val _batteryState = MutableStateFlow(getInitialBatteryState())
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let { updateFromIntent(it) }
        }
    }

    private var isRegistered = false

    fun startMonitoring() {
        if (!isRegistered) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val stickyIntent = context.registerReceiver(batteryReceiver, filter)
            stickyIntent?.let { updateFromIntent(it) }
            isRegistered = true
        }
    }

    fun stopMonitoring() {
        if (isRegistered) {
            try {
                context.unregisterReceiver(batteryReceiver)
            } catch (ignored: Exception) {
            }
            isRegistered = false
        }
    }

    private fun getInitialBatteryState(): BatteryState {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(null, filter)
        return if (intent != null) {
            parseBatteryIntent(intent)
        } else {
            BatteryState()
        }
    }

    private fun updateFromIntent(intent: Intent) {
        _batteryState.value = parseBatteryIntent(intent)
    }

    private fun parseBatteryIntent(intent: Intent): BatteryState {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) ((level * 100) / scale) else 100

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val plugType = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Battery"
        }

        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 250)
        val tempCelsius = tempTenths / 10.0f

        val isLow = percent <= 20 && !isCharging
        val isCritical = percent <= 10 && !isCharging

        return BatteryState(
            level = percent,
            isCharging = isCharging,
            chargePlug = plugType,
            temperatureCelsius = tempCelsius,
            isLowPower = isLow,
            isCriticalPower = isCritical
        )
    }
}
