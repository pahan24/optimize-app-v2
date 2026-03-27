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

        Handler(Looper.getMainLooper()).postDelayed({
            if (_binding != null) {
                if (SettingsManager.isLoggedIn(requireContext())) {
                    findNavController().navigate(R.id.action_splash_to_dashboard)
                } else {
                    findNavController().navigate(R.id.action_splash_to_login)
                }
            }
        }, 3000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
