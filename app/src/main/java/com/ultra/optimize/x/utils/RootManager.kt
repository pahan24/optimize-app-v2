package com.ultra.optimize.x.utils

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object RootManager {

    private const val TAG = "RootManager"

    fun isRooted(context: Context? = null): Boolean {
        if (context != null && SettingsManager.getSetting(context, SettingsManager.KEY_ROOT_MODE, false)) {
            return true
        }
        
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        
        for (path in paths) {
            if (java.io.File(path).exists()) return true
        }

        return try {
            val process = Runtime.getRuntime().exec("which su")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine() != null
        } catch (e: Exception) {
            false
        }
    }

    fun runCommand(command: String): String {
        if (ShizukuManager.isShizukuAvailable() && ShizukuManager.isPermissionGranted()) {
            return ShizukuManager.executeCommand(command)
        }

        var output = ""
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("$command\n")
            os.writeBytes("exit\n")
            os.flush()
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output += line + "\n"
            }
            process.waitFor()
        } catch (e: Exception) {
            Log.e(TAG, "Error running command: $command", e)
        }
        return output
    }

    fun execute(command: String) {
        runCommand(command)
    }

    fun requestRoot(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes("exit\n")
            os.flush()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}
