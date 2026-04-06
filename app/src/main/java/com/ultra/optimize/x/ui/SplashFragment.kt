package com.ultra.optimize.x.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ultra.optimize.x.R
import com.ultra.optimize.x.databinding.FragmentSplashBinding
import com.ultra.optimize.x.utils.SettingsManager
import com.ultra.optimize.x.utils.RootManager
import com.ultra.optimize.x.utils.ShizukuManager

import com.google.firebase.firestore.FirebaseFirestore
import com.ultra.optimize.x.utils.Constants
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = context ?: return
        // Set Spannable Title
        val title = "ULTRA\nOPTIMIZE X"
        val spannable = android.text.SpannableString(title)
        val blueColor = androidx.core.content.ContextCompat.getColor(context, R.color.neon_blue)
        spannable.setSpan(android.text.style.ForegroundColorSpan(blueColor), 6, 14, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        
        if (_binding != null) {
            binding.tvAppNameSplash.text = spannable

            // Simple animation
            binding.cvLogoSplash.alpha = 0f
            binding.cvLogoSplash.scaleX = 0.5f
            binding.cvLogoSplash.scaleY = 0.5f
            binding.cvLogoSplash.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(1200)
                .start()

            binding.tvAppNameSplash.alpha = 0f
            binding.tvAppNameSplash.translationY = 50f
            binding.tvAppNameSplash.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(1000)
                .setStartDelay(500)
                .start()

            checkForUpdates()
        }
    }

    private fun checkForUpdates() {
        val context = context ?: return
        
        // MANDATORY ACCESS CHECK: Shizuku or Root must be available
        val isRooted = RootManager.isRooted(context)
        val isShizukuAvailable = ShizukuManager.isShizukuAvailable()
        val isShizukuPermitted = ShizukuManager.isPermissionGranted()

        if (!isRooted && (!isShizukuAvailable || !isShizukuPermitted)) {
            showAccessRequiredDialog()
            return
        }

        val db = FirebaseFirestore.getInstance(Constants.FIRESTORE_DATABASE_ID)
        
        // Check System Status (Maintenance)
        db.collection("app_config").document("status")
            .get()
            .addOnSuccessListener { document ->
                if (_binding == null) return@addOnSuccessListener
                if (document != null && document.exists()) {
                    val maintenance = document.getBoolean("maintenance") ?: false
                    if (maintenance) {
                        showMaintenanceDialog()
                        return@addOnSuccessListener
                    }
                }
                
                // Continue with update check if not in maintenance
                fetchVersionInfo(db, context)
            }
            .addOnFailureListener {
                fetchVersionInfo(db, context)
            }
    }

    private fun fetchVersionInfo(db: FirebaseFirestore, context: android.content.Context) {
        db.collection("app_config").document("version_info")
            .get()
            .addOnSuccessListener { document ->
                if (_binding == null) return@addOnSuccessListener
                
                if (document != null && document.exists()) {
                    val latestCode = document.getLong("latest_version_code") ?: 0L
                    val currentCode = try {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
                    } catch (e: Exception) { 1L }
                    
                    val updateUrl = document.getString("update_url") ?: ""
                    val forceUpdate = document.getBoolean("update_required") ?: false
                    
                    if (latestCode > currentCode) {
                        showUpdateDialog(updateUrl, forceUpdate)
                    } else {
                        proceed()
                    }
                } else {
                    proceed()
                }
            }
            .addOnFailureListener {
                proceed()
            }
    }

    private fun showMaintenanceDialog() {
        val context = context ?: return
        AlertDialog.Builder(context, R.style.NeonDialogTheme)
            .setTitle("System Maintenance")
            .setMessage("Ultra Optimize X is currently under maintenance. Please try again later.")
            .setCancelable(false)
            .setPositiveButton("EXIT") { _, _ -> activity?.finish() }
            .setNeutralButton("RETRY") { _, _ -> checkForUpdates() }
            .show()
    }

    private fun showAccessRequiredDialog() {
        val context = context ?: return
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_shizuku_tutorial_video, null)
        val dialog = AlertDialog.Builder(context, R.style.NeonDialogTheme)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val videoView = dialogView.findViewById<android.widget.VideoView>(R.id.vv_tutorial)
        val progressBar = dialogView.findViewById<android.widget.ProgressBar>(R.id.pb_video_loading)
        
        // Shizuku Tutorial Video URL (Placeholder or actual URL if available)
        val videoUrl = "https://firebasestorage.googleapis.com/v0/b/ultra-optimize-x.appspot.com/o/shizuku_tutorial.mp4?alt=media"
        
        videoView.setVideoURI(Uri.parse(videoUrl))
        videoView.setOnPreparedListener { mp ->
            mp.isLooping = true
            progressBar.visibility = View.GONE
            videoView.start()
        }
        
        videoView.setOnErrorListener { _, _, _ ->
            progressBar.visibility = View.GONE
            android.widget.Toast.makeText(context, "Error loading tutorial video", android.widget.Toast.LENGTH_SHORT).show()
            true
        }

        dialogView.findViewById<View>(R.id.btn_close_tutorial).setOnClickListener {
            dialog.dismiss()
            checkForUpdates()
        }

        dialogView.findViewById<View>(R.id.btn_open_shizuku_tutorial).setOnClickListener {
            try {
                val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                if (intent != null) {
                    startActivity(intent)
                } else {
                    val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=moe.shizuku.privileged.api"))
                    startActivity(playStoreIntent)
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Shizuku not found", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        dialog.setOnDismissListener {
            videoView.stopPlayback()
        }

        dialog.show()
    }

    private fun showUpdateDialog(url: String, force: Boolean) {
        val context = context ?: return
        val builder = AlertDialog.Builder(context, R.style.NeonDialogTheme)
            .setTitle("Update Available")
            .setMessage("A new version of Ultra Optimize X is available. Please update to continue.")
            .setPositiveButton("UPDATE") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
                if (force) activity?.finish()
            }
            
        if (!force) {
            builder.setNegativeButton("LATER") { _, _ -> proceed() }
        } else {
            builder.setCancelable(false)
        }
        
        builder.show()
    }

    private fun proceed() {
        val context = context ?: return
        Handler(Looper.getMainLooper()).postDelayed({
            if (_binding != null) {
                if (SettingsManager.isLoggedIn(context)) {
                    findNavController().navigate(R.id.action_splash_to_dashboard)
                } else {
                    findNavController().navigate(R.id.action_splash_to_login)
                }
            }
        }, 1500)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
