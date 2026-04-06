package com.ultra.optimize.x.utils

import android.content.Context
import android.util.Log

object FreeFireManager {

    private const val TAG = "FreeFireManager"

    fun optimizeForFreeFire(context: Context, isRooted: Boolean) {
        // 1. Deep RAM Clean
        RamManager.boostRam(context, true)
        
        if (isRooted) {
            applyRootOptimizations()
        } else {
            applyNonRootOptimizations(context)
        }
    }

    private fun applyRootOptimizations() {
        val commands = arrayOf(
            "echo performance > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor",
            "echo performance > /sys/devices/system/cpu/cpu1/cpufreq/scaling_governor",
            "echo performance > /sys/devices/system/cpu/cpu2/cpufreq/scaling_governor",
            "echo performance > /sys/devices/system/cpu/cpu3/cpufreq/scaling_governor",
            "sysctl -w net.ipv4.tcp_low_latency=1",
            "sysctl -w net.ipv4.tcp_timestamps=0",
            "setprop debug.performance.tuning 1",
            "setprop video.accelerate.hw 1"
        )
        
        for (cmd in commands) {
            if (ShizukuManager.isShizukuAvailable() && ShizukuManager.isPermissionGranted()) {
                ShizukuManager.executeCommand(cmd)
            } else {
                RootManager.execute(cmd)
            }
        }
        
        Log.d(TAG, "Free Fire optimizations applied via Root/Shizuku")
    }

    private fun applyNonRootOptimizations(context: Context) {
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.isPermissionGranted()) {
            applyRootOptimizations()
        } else {
            Log.d(TAG, "Non-rooted Free Fire optimizations applied (RAM only)")
        }
    }
}
