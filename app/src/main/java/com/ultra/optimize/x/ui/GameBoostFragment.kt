package com.ultra.optimize.x.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ultra.optimize.x.R
import com.ultra.optimize.x.databinding.FragmentGameBoostBinding
import com.ultra.optimize.x.utils.CpuManager
import com.ultra.optimize.x.utils.RamManager
import com.ultra.optimize.x.utils.RootManager

class GameBoostFragment : Fragment() {
    private var _binding: FragmentGameBoostBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateStats()
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGameBoostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        // Set Spannable Title
        val title = "GAME\nBOOST"
        val spannable = android.text.SpannableString(title)
        val blueColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.neon_blue)
        spannable.setSpan(android.text.style.ForegroundColorSpan(blueColor), 0, 4, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tvTitle.text = spannable
        
        binding.btnActivate.setOnClickListener {
            binding.btnActivate.isEnabled = false
            binding.btnActivate.text = "INITIALIZING..."
            
            Thread {
                val isRooted = RootManager.isRooted(requireContext())
                
                handler.post { binding.tvBoostStatus.text = "OPTIMIZING RAM..." }
                RamManager.boostRam(requireContext(), isRooted)
                Thread.sleep(800)
                
                handler.post { binding.tvBoostStatus.text = "TUNING CPU CORES..." }
                if (isRooted || (ShizukuManager.isShizukuAvailable() && ShizukuManager.isPermissionGranted())) {
                    CpuManager.setGovernor("performance")
                }
                Thread.sleep(800)
                
                handler.post { binding.tvBoostStatus.text = "CLEANING BACKGROUND..." }
                Thread.sleep(800)
                
                activity?.runOnUiThread {
                    if (_binding != null) {
                        binding.tvBoostStatus.text = "GAME MODE: ACTIVE"
                        binding.btnActivate.text = "GAME MODE ACTIVE"
                        binding.tvBoostStatus.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.accent_green))
                        Toast.makeText(requireContext(), "Game Mode Activated!", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }

        handler.post(updateRunnable)
    }

    private fun updateStats() {
        if (_binding == null) return
        val ramUsage = RamManager.getRamUsage(requireContext())
        animateProgress(binding.progressRamGame, ramUsage)
    }

    private fun animateProgress(progress: com.google.android.material.progressindicator.CircularProgressIndicator, value: Int) {
        val animator = android.animation.ValueAnimator.ofInt(progress.progress, value)
        animator.duration = 800
        animator.interpolator = android.view.animation.DecelerateInterpolator()
        animator.addUpdateListener {
            if (_binding != null) {
                progress.progress = it.animatedValue as Int
            }
        }
        animator.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateRunnable)
        _binding = null
    }
}
