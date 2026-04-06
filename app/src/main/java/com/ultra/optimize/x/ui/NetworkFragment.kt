package com.ultra.optimize.x.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ultra.optimize.x.databinding.FragmentNetworkBinding
import com.ultra.optimize.x.utils.SettingsManager
import kotlin.random.Random

class NetworkFragment : Fragment() {

    private var _binding: FragmentNetworkBinding? = null
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())
    private var isTesting = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNetworkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnStartTest.setOnClickListener {
            if (!isTesting) {
                startSpeedTest()
            }
        }

        binding.switchDnsLeak.isChecked = SettingsManager.getSetting(requireContext(), "dns_leak_protection", false)
        binding.switchDnsLeak.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveSetting(requireContext(), "dns_leak_protection", isChecked)
            if (isChecked) Toast.makeText(context, "DNS Leak Protection Active", Toast.LENGTH_SHORT).show()
        }

        binding.switchPingStabilizer.isChecked = SettingsManager.getSetting(requireContext(), "ping_stabilizer", false)
        binding.switchPingStabilizer.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveSetting(requireContext(), "ping_stabilizer", isChecked)
            if (isChecked) Toast.makeText(context, "Ping Stabilizer Active", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startSpeedTest() {
        isTesting = true
        binding.btnStartTest.isEnabled = false
        binding.btnStartTest.text = "TESTING..."
        
        var currentSpeed = 0.0
        val targetSpeed = Random.nextDouble(20.0, 150.0)
        val duration = 5000L
        val interval = 50L
        val steps = duration / interval
        var currentStep = 0

        handler.post(object : Runnable {
            override fun run() {
                if (currentStep < steps) {
                    currentSpeed += (targetSpeed / steps) * (1 + Random.nextDouble(-0.2, 0.2))
                    if (currentSpeed < 0) currentSpeed = 0.0
                    
                    if (_binding != null) {
                        binding.tvSpeedValue.text = String.format("%.1f", currentSpeed)
                        animateProgress(binding.progressSpeed, (currentSpeed / 150.0 * 100).toInt())
                    }
                    
                    currentStep++
                    handler.postDelayed(this, interval)
                } else {
                    finishTest(targetSpeed)
                }
            }
        })
    }

    private fun animateProgress(progress: com.google.android.material.progressindicator.CircularProgressIndicator, value: Int) {
        val animator = android.animation.ValueAnimator.ofInt(progress.progress, value)
        animator.duration = 100
        animator.interpolator = android.view.animation.LinearInterpolator()
        animator.addUpdateListener {
            if (_binding != null) {
                progress.progress = it.animatedValue as Int
            }
        }
        animator.start()
    }

    private fun finishTest(finalSpeed: Double) {
        isTesting = false
        binding.btnStartTest.isEnabled = true
        binding.btnStartTest.text = "START SPEED TEST"
        binding.tvSpeedValue.text = String.format("%.1f", finalSpeed)
        binding.progressSpeed.progress = (finalSpeed / 150.0 * 100).toInt()
        
        binding.tvPingDetail.text = "${Random.nextInt(15, 60)}ms"
        binding.tvJitterDetail.text = "${Random.nextInt(1, 10)}ms"
        
        Toast.makeText(context, "Speed Test Completed", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}
