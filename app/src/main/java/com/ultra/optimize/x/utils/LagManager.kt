package com.ultra.optimize.x.utils

import android.content.Context
import android.provider.Settings
import android.util.Log

object LagManager {

    private const val TAG = "LagManager"

    fun fixLag(context: Context, isRooted: Boolean) {
        if (isRooted) {
            applyRootLagFix()
        } else {
            applyNonRootLagFix(context)
        }
    }

    private fun applyRootLagFix() {
        // Rooted: Set animation scales to 0.5x or 0x for instant UI
        RootManager.execute("settings put global window_animation_scale 0.5")
        RootManager.execute("settings put global transition_animation_scale 0.5")
        RootManager.execute("settings put global animator_duration_scale 0.5")
        
        // Force GPU rendering (if possible via shell)
        RootManager.execute("setprop debug.hwui.renderer opengl")
        RootManager.execute("setprop persist.sys.ui.hw true")
        
        Log.d(TAG, "Rooted lag fix applied")
    }

    private fun applyNonRootLagFix(context: Context) {
        // Non-rooted: We can't change secure settings easily, but we can clear some cache and kill background apps
        RamManager.boostRam(context, false)
        Log.d(TAG, "Non-rooted lag fix applied (RAM boost only)")
    }
}
