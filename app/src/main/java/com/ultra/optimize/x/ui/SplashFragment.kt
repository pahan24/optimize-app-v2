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

        // Set Spannable Title
        val title = "ULTRA\nOPTIMIZE X"
        val spannable = android.text.SpannableString(title)
        val blueColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.neon_blue)
        spannable.setSpan(android.text.style.ForegroundColorSpan(blueColor), 6, 14, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
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

    private fun checkForUpdates() {
        val db = FirebaseFirestore.getInstance(Constants.FIRESTORE_DATABASE_ID)
        db.collection("app_config").document("version_info")
            .get()
            .addOnSuccessListener { document ->
                if (_binding == null) return@addOnSuccessListener
                
                if (document != null && document.exists()) {
                    val latestCode = document.getLong("latest_version_code") ?: 0L
                    val currentCode = try {
                        requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionCode.toLong()
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

    private fun showUpdateDialog(url: String, force: Boolean) {
        val builder = AlertDialog.Builder(requireContext(), R.style.NeonDialogTheme)
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
        Handler(Looper.getMainLooper()).postDelayed({
            if (_binding != null) {
                if (SettingsManager.isLoggedIn(requireContext())) {
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
