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
        
        binding.btnActivate.setOnClickListener {
            binding.btnActivate.isEnabled = false
            binding.btnActivate.text = "ACTIVATING..."
            
            Thread {
                RamManager.boostRam(requireContext(), RootManager.isRooted())
                if (RootManager.isRooted()) {
                    CpuManager.setGovernor("performance")
                }
                Thread.sleep(1000)
                activity?.runOnUiThread {
                    if (_binding != null) {
                        binding.tvBoostStatus.text = "Game Mode: Active"
                        binding.btnActivate.text = "GAME MODE ACTIVE"
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
        binding.progressRamGame.setProgress(ramUsage, true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateRunnable)
        _binding = null
    }
}
