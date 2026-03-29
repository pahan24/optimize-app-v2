package com.ultra.optimize.x.utils

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ultra_optimize_prefs", Context.MODE_PRIVATE)

    fun setFeatureEnabled(featureKey: String, enabled: Boolean) {
        prefs.edit().putBoolean(featureKey, enabled).apply()
    }

    fun isFeatureEnabled(featureKey: String, defaultValue: Boolean = false): Boolean {
        return prefs.getBoolean(featureKey, defaultValue)
    }

    companion object {
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_AUTO_BOOST = "auto_boost_enabled"
        const val KEY_ROOT_MODE = "root_mode_forced"
        const val KEY_BATTERY_SAVER = "battery_saver_enabled"
        const val KEY_GAME_MODE = "game_mode_enabled"
        const val KEY_NETWORK_OPT = "network_opt_enabled"
        const val KEY_DISPLAY_TWEAKS = "display_tweaks_enabled"
        const val KEY_KERNEL_TWEAKS = "kernel_tweaks_enabled"
        const val KEY_DEBLOATER = "debloater_enabled"
        const val KEY_DNS_CHANGER = "dns_changer_enabled"
        const val KEY_CHARGE_BOOST = "charge_boost_enabled"
        const val KEY_AUTO_CLEAN = "auto_clean_enabled"
        const val KEY_FPS_METER = "fps_meter_enabled"
        const val KEY_COOL_DOWN = "cool_down_enabled"
        const val KEY_DARK_MODE = "dark_mode_enabled"
        const val KEY_IS_ADMIN = "is_admin"
        const val KEY_IS_LOGGED_IN = "is_logged_in"

        fun getSetting(context: Context, key: String, defaultValue: Boolean = false): Boolean {
            val prefs = context.getSharedPreferences("ultra_optimize_prefs", Context.MODE_PRIVATE)
            return prefs.getBoolean(key, defaultValue)
        }

        fun saveSetting(context: Context, key: String, value: Boolean) {
            val prefs = context.getSharedPreferences("ultra_optimize_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean(key, value).apply()
        }

        fun isLoggedIn(context: Context): Boolean = getSetting(context, KEY_IS_LOGGED_IN, false)
        fun setLoggedIn(context: Context, loggedIn: Boolean) = saveSetting(context, KEY_IS_LOGGED_IN, loggedIn)
        
        fun isAdmin(context: Context): Boolean = getSetting(context, KEY_IS_ADMIN, false)
        fun setAdmin(context: Context, isAdmin: Boolean) = saveSetting(context, KEY_IS_ADMIN, isAdmin)

        fun isDarkMode(context: Context): Boolean = getSetting(context, KEY_DARK_MODE, true)
        fun setDarkMode(context: Context, value: Boolean) = saveSetting(context, KEY_DARK_MODE, value)
    }
}
