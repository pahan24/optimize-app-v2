package com.ultra.optimize.x.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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

        val settingsManager = com.ultra.optimize.x.utils.SettingsManager(requireContext())
        
        binding.switchNotifications.isChecked = settingsManager.isFeatureEnabled(com.ultra.optimize.x.utils.SettingsManager.KEY_NOTIFICATIONS, true)
        binding.switchAutoBoost.isChecked = settingsManager.isFeatureEnabled(com.ultra.optimize.x.utils.SettingsManager.KEY_AUTO_BOOST)
        binding.switchRootMode.isChecked = settingsManager.isFeatureEnabled(com.ultra.optimize.x.utils.SettingsManager.KEY_ROOT_MODE)

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setFeatureEnabled(com.ultra.optimize.x.utils.SettingsManager.KEY_NOTIFICATIONS, isChecked)
        }
        binding.switchAutoBoost.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setFeatureEnabled(com.ultra.optimize.x.utils.SettingsManager.KEY_AUTO_BOOST, isChecked)
        }
        binding.switchRootMode.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setFeatureEnabled(com.ultra.optimize.x.utils.SettingsManager.KEY_ROOT_MODE, isChecked)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
