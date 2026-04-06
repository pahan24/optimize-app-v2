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
        val commands = arrayOf(
            "settings put global window_animation_scale 0.5",
            "settings put global transition_animation_scale 0.5",
            "settings put global animator_duration_scale 0.5",
            "setprop debug.hwui.renderer opengl",
            "setprop persist.sys.ui.hw true"
        )
        
        for (cmd in commands) {
            if (ShizukuManager.isShizukuAvailable() && ShizukuManager.isPermissionGranted()) {
                ShizukuManager.executeCommand(cmd)
            } else {
                RootManager.execute(cmd)
            }
        }
        
        Log.d(TAG, "Lag fix applied via Root/Shizuku")
    }

    private fun applyNonRootLagFix(context: Context) {
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.isPermissionGranted()) {
            applyRootLagFix()
        } else {
            RamManager.boostRam(context, false)
            Log.d(TAG, "Non-rooted lag fix applied (RAM boost only)")
        }
    }
}
