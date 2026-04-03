package com.ultra.optimize.x.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider

object ShizukuManager {
    private const val TAG = "ShizukuManager"
    const val REQUEST_CODE = 1001

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun isPermissionGranted(): Boolean {
        return if (Shizuku.isPreV11()) {
            // Pre-v11 Shizuku doesn't have runtime permission
            true
        } else {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestPermission(listener: Shizuku.OnRequestPermissionResultListener) {
        if (isShizukuAvailable()) {
            Shizuku.addRequestPermissionResultListener(listener)
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }

    fun removePermissionListener(listener: Shizuku.OnRequestPermissionResultListener) {
        Shizuku.removeRequestPermissionResultListener(listener)
    }

    fun executeCommand(command: String): String {
        if (!isShizukuAvailable() || !isPermissionGranted()) {
            return "Shizuku not available or permission not granted"
        }

        return try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val error = process.errorStream.bufferedReader().use { it.readText() }
            process.waitFor()
            
            if (error.isNotEmpty()) {
                "Error: $error"
            } else {
                output
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute command via Shizuku", e)
            "Exception: ${e.message}"
        }
    }
}
