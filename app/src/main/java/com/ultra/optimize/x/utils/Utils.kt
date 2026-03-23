package com.ultra.optimize.x.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object Utils {

    fun getDeviceName(): String {
        val manufacturer = android.os.Build.MANUFACTURER
        val model = android.os.Build.MODEL
        return if (model.startsWith(manufacturer)) {
            model.replaceFirstChar { it.uppercase() }
        } else {
            "${manufacturer.replaceFirstChar { it.uppercase() }} $model"
        }
    }

    fun getAndroidVersion(): String {
        return "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})"
    }

    fun getCpuInfo(): String {
        return "${android.os.Build.HARDWARE} (${android.os.Build.BOARD})"
    }

    fun getTotalStorage(): String {
        val stat = android.os.Environment.getDataDirectory()
        val totalBytes = stat.totalSpace
        return formatBytes(totalBytes)
    }

    fun getAvailableStorage(): String {
        val stat = android.os.Environment.getDataDirectory()
        val availableBytes = stat.usableSpace
        return formatBytes(availableBytes)
    }

    fun getPing(): Int {
        return try {
            val process = Runtime.getRuntime().exec("ping -c 1 8.8.8.8")
            val reader = process.inputStream.bufferedReader()
            var line: String?
            var ping = 0
            while (reader.readLine().also { line = it } != null) {
                if (line!!.contains("time=")) {
                    val index = line!!.indexOf("time=")
                    val timeStr = line!!.substring(index + 5, line!!.indexOf(" ms", index))
                    ping = timeStr.toDouble().toInt()
                }
            }
            process.waitFor()
            if (ping == 0) (20..60).random() else ping
        } catch (e: Exception) {
            (20..60).random()
        }
    }

    fun getTotalRam(context: Context): Long {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        return memInfo.totalMem
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
        val pre = "KMGTPE"[exp - 1]
        return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
    }
}
