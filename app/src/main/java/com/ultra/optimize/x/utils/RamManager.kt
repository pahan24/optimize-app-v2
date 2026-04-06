package com.ultra.optimize.x.utils

import android.app.ActivityManager
import android.content.Context
import android.util.Log

object RamManager {

    fun getRamUsage(context: Context): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalRam = memoryInfo.totalMem
        val availableRam = memoryInfo.availMem
        val usedRam = totalRam - availableRam
        
        return ((usedRam.toDouble() / totalRam.toDouble()) * 100).toInt()
    }

    fun boostRam(context: Context, isRoot: Boolean) {
        if (isRoot || (ShizukuManager.isShizukuAvailable() && ShizukuManager.isPermissionGranted())) {
            RootManager.runCommand("sync; echo 3 > /proc/sys/vm/drop_caches")
            // Kill background processes
            RootManager.runCommand("am kill-all")
        } else {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningAppProcesses = activityManager.runningAppProcesses
            if (runningAppProcesses != null) {
                for (processInfo in runningAppProcesses) {
                    if (processInfo.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE) {
                        for (pkgName in processInfo.pkgList) {
                            activityManager.killBackgroundProcesses(pkgName)
                        }
                    }
                }
            }
        }
    }
}
