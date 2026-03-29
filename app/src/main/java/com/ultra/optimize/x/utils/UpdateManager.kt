package com.ultra.optimize.x.utils

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.ultra.optimize.x.BuildConfig
import com.ultra.optimize.x.R
import java.io.File

/**
 * UpdateManager handles checking for application updates via Firebase Firestore.
 * It compares the current version code with the latest version code stored in the database.
 */
class UpdateManager(private val activity: Activity) {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "UpdateManager"
    private val PREFS_NAME = "update_prefs"
    private val KEY_LAST_CHECK_TIME = "last_check_time"
    private val CHECK_INTERVAL = 0 // 0 for testing, change to 1 hour (1000 * 60 * 60) for production

    /**
     * Checks for updates. If a new version is available, it shows a dialog.
     * @param forceCheck If true, it will bypass the time interval check.
     */
    fun checkForUpdates(forceCheck: Boolean = false) {
        Log.d(TAG, "checkForUpdates called")
        Toast.makeText(activity, "Checking for updates...", Toast.LENGTH_SHORT).show()

        if (!forceCheck && !shouldCheckForUpdate()) {
            Log.d(TAG, "Skipping update check (interval not reached)")
            Toast.makeText(activity, "Skipping update check (interval not reached)", Toast.LENGTH_SHORT).show()
            return
        }

        val loadingDialog = showLoadingDialog()
        Log.d(TAG, "Loading dialog shown")

        // Fetch version info from Firestore
        db.collection("app_config").document("version_info")
            .get()
            .addOnSuccessListener { document ->
                loadingDialog.dismiss()
                Log.d(TAG, "Firestore success: ${document.exists()}")
                if (document.exists()) {
                    // Fetch version info from Firestore
                    val latestVersionCode = when (val value = document.get("latest_version_code")) {
                        is Number -> value.toLong()
                        is String -> value.toLongOrNull() ?: 0
                        else -> 0
                    }
                    val latestVersionName = document.getString("latest_version_name") ?: ""
                    val updateRequired = document.getBoolean("update_required") ?: false
                    val updateUrl = document.getString("update_url") ?: ""

                    val currentVersionCode = BuildConfig.VERSION_CODE
                    Log.d(TAG, "Latest Version: $latestVersionCode, Current: $currentVersionCode")
                    Toast.makeText(activity, "Latest: $latestVersionCode, Current: $currentVersionCode", Toast.LENGTH_LONG).show()

                    // Update last check time
                    saveLastCheckTime()

                    if (latestVersionCode > currentVersionCode) {
                        Log.d(TAG, "Showing update dialog")
                        showUpdateDialog(latestVersionName, updateRequired, updateUrl)
                    } else {
                        Log.d(TAG, "App is up to date.")
                        Toast.makeText(activity, "App is up to date.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e(TAG, "Document 'version_info' does not exist in 'app_config' collection.")
                    Toast.makeText(activity, "Error: Document 'version_info' not found in 'app_config'", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Log.e(TAG, "Update check failed: ${e.message}", e)
                Toast.makeText(activity, "Update check failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun shouldCheckForUpdate(): Boolean {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheckTime = prefs.getLong(KEY_LAST_CHECK_TIME, 0)
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastCheckTime) > CHECK_INTERVAL
    }

    private fun saveLastCheckTime() {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis()).apply()
    }

    private fun showLoadingDialog(): AlertDialog {
        val builder = MaterialAlertDialogBuilder(activity, R.style.Theme_UltraOptimizeX_Dialog)
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_loading_update, null)
        builder.setView(view)
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.show()
        return dialog
    }

    private fun showUpdateDialog(latestVersionName: String, updateRequired: Boolean, updateUrl: String) {
        val builder = MaterialAlertDialogBuilder(activity, R.style.Theme_UltraOptimizeX_Dialog)
        builder.setTitle(if (updateRequired) "Update Required" else "Update Available")
        builder.setMessage(
            if (updateRequired) 
                "A new version ($latestVersionName) is required to continue using the app. Please update now."
            else 
                "A new version ($latestVersionName) is available. Would you like to update now?"
        )
        
        builder.setPositiveButton("Update Now") { _, _ ->
            startDownload(updateUrl)
            if (updateRequired) {
                Toast.makeText(activity, "Update is required. App will close during installation.", Toast.LENGTH_LONG).show()
            }
        }

        if (updateRequired) {
            builder.setCancelable(false)
        } else {
            builder.setNegativeButton("Later", null)
            builder.setCancelable(true)
        }

        builder.show()
    }

    private fun startDownload(url: String) {
        if (url.isEmpty()) {
            Toast.makeText(activity, "Invalid update URL", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(activity, "Downloading update...", Toast.LENGTH_LONG).show()

        val fileName = "UltraOptimizeX_Update.apk"
        val file = File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (file.exists()) file.delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Downloading Update")
            .setDescription("Ultra Optimize X is updating...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationUri(Uri.fromFile(file))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        // Register receiver to know when download is done
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    installApk(file)
                    activity.unregisterReceiver(this)
                }
            }
        }
        activity.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun installApk(file: File) {
        try {
            val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Installation failed", e)
            Toast.makeText(activity, "Installation failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
