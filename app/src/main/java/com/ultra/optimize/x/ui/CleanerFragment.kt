package com.ultra.optimize.x.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ultra.optimize.x.databinding.FragmentCleanerBinding

class CleanerFragment : Fragment() {
    private var _binding: FragmentCleanerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCleanerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        
        // Set initial progress
        binding.progressStorage.setProgress(75, true)

        binding.btnClean.setOnClickListener {
            binding.btnClean.isEnabled = false
            binding.btnClean.text = "CLEANING..."
            
            Thread {
                Thread.sleep(2000)
                activity?.runOnUiThread {
                    if (_binding != null) {
                        binding.tvJunkSize.text = "0 MB"
                        binding.progressStorage.setProgress(0, true)
                        binding.btnClean.text = "SYSTEM CLEAN"
                        Toast.makeText(requireContext(), "Storage Cleaned!", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
