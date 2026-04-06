package com.ultra.optimize.x.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ultra.optimize.x.R
import com.ultra.optimize.x.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        val context = context ?: return
        // Set Spannable Title
        val title = "SYSTEM\nSETTINGS"
        val spannable = android.text.SpannableString(title)
        val blueColor = androidx.core.content.ContextCompat.getColor(context, R.color.neon_blue)
        spannable.setSpan(android.text.style.ForegroundColorSpan(blueColor), 0, 6, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        
        if (_binding != null) {
            binding.tvTitle.text = spannable

            val settingsManager = com.ultra.optimize.x.utils.SettingsManager(context)
            
            binding.switchNotifications.isChecked = settingsManager.isFeatureEnabled(com.ultra.optimize.x.utils.SettingsManager.KEY_NOTIFICATIONS, true)
            binding.switchAutoBoost.isChecked = settingsManager.isFeatureEnabled(com.ultra.optimize.x.utils.SettingsManager.KEY_AUTO_BOOST)
            binding.switchRootMode.isChecked = settingsManager.isFeatureEnabled(com.ultra.optimize.x.utils.SettingsManager.KEY_ROOT_MODE)
            binding.switchDarkMode.isChecked = settingsManager.isFeatureEnabled(com.ultra.optimize.x.utils.SettingsManager.KEY_DARK_MODE, true)

            binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
                settingsManager.setFeatureEnabled(com.ultra.optimize.x.utils.SettingsManager.KEY_NOTIFICATIONS, isChecked)
            }
            binding.switchAutoBoost.setOnCheckedChangeListener { _, isChecked ->
                settingsManager.setFeatureEnabled(com.ultra.optimize.x.utils.SettingsManager.KEY_AUTO_BOOST, isChecked)
            }
            binding.switchRootMode.setOnCheckedChangeListener { _, isChecked ->
                settingsManager.setFeatureEnabled(com.ultra.optimize.x.utils.SettingsManager.KEY_ROOT_MODE, isChecked)
                if (isChecked) {
                    android.widget.Toast.makeText(context, "Simulated Root Mode Enabled", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

            binding.btnCheckRoot.setOnClickListener {
                val isRooted = com.ultra.optimize.x.utils.RootManager.isRooted(context)
                val message = if (isRooted) "System is ROOTED (Authorized)" else "System is NOT ROOTED (Denied)"
                
                com.google.android.material.dialog.MaterialAlertDialogBuilder(context, R.style.Theme_UltraOptimizeX)
                    .setTitle("Root Status")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            }

            binding.btnCheckUpdate.setOnClickListener {
                checkForUpdatesManual()
            }

            binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
                settingsManager.setFeatureEnabled(com.ultra.optimize.x.utils.SettingsManager.KEY_DARK_MODE, isChecked)
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    if (isChecked) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                )
            }

            if (com.ultra.optimize.x.utils.SettingsManager.isAdmin(context)) {
                binding.btnAdminPanel.visibility = View.VISIBLE
                binding.btnAdminPanel.setOnClickListener {
                    findNavController().navigate(R.id.action_settings_to_admin)
                }
            }

            binding.btnLogout.setOnClickListener {
                com.ultra.optimize.x.utils.SettingsManager.setLoggedIn(context, false)
                findNavController().navigate(R.id.action_settings_to_login)
            }
        }
    }

    private fun checkForUpdatesManual() {
        val context = context ?: return
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance(com.ultra.optimize.x.utils.Constants.FIRESTORE_DATABASE_ID)
        db.collection("app_config").document("version_info")
            .get()
            .addOnSuccessListener { document ->
                if (_binding == null) return@addOnSuccessListener
                if (document != null && document.exists()) {
                    val latestCode = document.getLong("latest_version_code") ?: 0L
                    val currentCode = try {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
                    } catch (e: Exception) { 1L }
                    
                    if (latestCode > currentCode) {
                        val url = document.getString("update_url") ?: ""
                        com.google.android.material.dialog.MaterialAlertDialogBuilder(context, R.style.Theme_UltraOptimizeX)
                            .setTitle("Update Available")
                            .setMessage("A new version is available. Would you like to update now?")
                            .setPositiveButton("UPDATE") { _, _ ->
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                startActivity(intent)
                            }
                            .setNegativeButton("LATER", null)
                            .show()
                    } else {
                        android.widget.Toast.makeText(context, "You are on the latest version!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
