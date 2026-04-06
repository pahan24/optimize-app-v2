package com.ultra.optimize.x.utils

import java.io.File
import com.ultra.optimize.x.utils.SettingsManager

object ThermalManager {

    fun getBatteryTemp(context: android.content.Context): Float {
        val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val temp = intent?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        return temp.toFloat() / 10
    }

    fun getCpuTemp(): Float {
        val thermalPaths = arrayOf(
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/devices/virtual/thermal/thermal_zone0/temp",
            "/sys/kernel/debug/tegra_thermal/temp_tj"
        )
        
        for (path in thermalPaths) {
            try {
                val tempStr = RootManager.runCommand("cat $path").trim()
                if (tempStr.isNotEmpty()) {
                    val temp = tempStr.toFloat()
                    return if (temp > 1000) temp / 1000 else temp
                }
            } catch (e: Exception) {
                continue
            }
        }
        return 0f
    }

    fun getBatteryCurrent(context: android.content.Context): Int {
        val batteryManager = context.getSystemService(android.content.Context.BATTERY_SERVICE) as android.os.BatteryManager
        return batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000 // Convert to mA
    }

    fun isAutoCoolDownEnabled(context: android.content.Context): Boolean {
        return SettingsManager.getSetting(context, "auto_cool_down")
    }

    fun setAutoCoolDownEnabled(context: android.content.Context, enabled: Boolean) {
        SettingsManager.saveSetting(context, "auto_cool_down", enabled)
    }
}
