package com.ultra.optimize.x.ui

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ultra.optimize.x.R
import com.ultra.optimize.x.databinding.FragmentDashboardBinding
import com.ultra.optimize.x.utils.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private var isRooted = false

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateStats()
            handler.postDelayed(this, 2000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupDeviceInfo()
        checkRoot()
        setupListeners()
        
        handler.post(updateRunnable)
    }

    private fun setupDeviceInfo() {
        val deviceName = Utils.getDeviceName()
        val androidVer = Utils.getAndroidVersion()
        val cpuInfo = Utils.getCpuInfo()
        val totalStorage = Utils.getTotalStorage()
        val availableStorage = Utils.getAvailableStorage()
        val totalRam = Utils.formatBytes(Utils.getTotalRam(requireContext()))

        binding.tvDeviceName.text = "Device: $deviceName"
        binding.tvAndroidVersion.text = androidVer
        binding.tvCpuInfo.text = "CPU: $cpuInfo"
        binding.tvStorageInfo.text = "Storage: $availableStorage / $totalStorage"
        
        // Add RAM info to device info card as well
        val ramInfo = "RAM: $totalRam"
        // We can append it to android version or add another textview. 
        // Let's just update the existing ones for now.
    }

    private fun checkRoot() {
        isRooted = RootManager.isRooted()
        if (isRooted) {
            binding.tvRootStatus.text = "Root Access: Enabled"
            binding.tvRootStatus.setTextColor(resources.getColor(R.color.accent_green))
        } else {
            binding.tvRootStatus.text = "Root Access: Disabled"
            binding.tvRootStatus.setTextColor(resources.getColor(R.color.accent_red))
        }
    }

    private fun setupListeners() {
        binding.btnBoost.setOnClickListener {
            performBoost()
        }

        binding.cardGameBoost.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_gameBoost)
        }

        binding.cardCpuControl.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_cpuControl)
        }

        binding.cardStorageCleaner.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_cleaner)
        }
        
        binding.cardThermalControl.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_thermalControl)
        }

        binding.cardBatterySaver.setOnClickListener {
            navigateWithTitle("Battery Saver", R.id.action_dashboard_to_batterySaver)
        }

        binding.cardAppManager.setOnClickListener {
            navigateWithTitle("App Manager", R.id.action_dashboard_to_appManager)
        }

        binding.cardNetworkOptimizer.setOnClickListener {
            navigateWithTitle("Network Optimizer", R.id.action_dashboard_to_networkOpt)
        }

        binding.cardDisplayTweaks.setOnClickListener {
            navigateWithTitle("Display Tweaks", R.id.action_dashboard_to_displayTweaks)
        }

        binding.cardKernelTweaks.setOnClickListener {
            navigateWithTitle("Kernel Tweaks", R.id.action_dashboard_to_kernelTweaks)
        }

        binding.cardSystemDebloater.setOnClickListener {
            navigateWithTitle("System Debloater", R.id.action_dashboard_to_debloater)
        }

        binding.cardDnsChanger.setOnClickListener {
            navigateWithTitle("DNS Changer", R.id.action_dashboard_to_dnsChanger)
        }

        binding.cardChargingBooster.setOnClickListener {
            navigateWithTitle("Charging Booster", R.id.action_dashboard_to_chargeBoost)
        }

        binding.cardAutoClean.setOnClickListener {
            navigateWithTitle("Auto Clean", R.id.action_dashboard_to_autoClean)
        }

        binding.cardFpsMeter.setOnClickListener {
            navigateWithTitle("FPS Meter", R.id.action_dashboard_to_fpsMeter)
        }

        binding.cardSettings.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_settings)
        }

        binding.cardLagFixer.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_lagFixer)
        }

        binding.cardFreeFire.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_freeFire)
        }

        binding.cardGameTools.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_gameTools)
        }

        binding.tvAppTitle.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_settings)
        }
    }

    private fun navigateWithTitle(title: String, actionId: Int) {
        val bundle = Bundle().apply {
            putString("featureTitle", title)
        }
        findNavController().navigate(actionId, bundle)
    }

    private fun updateStats() {
        if (_binding == null) return
        
        val ramUsage = RamManager.getRamUsage(requireContext())
        binding.progressRamCircular.setProgress(ramUsage, true)
        binding.tvRamValueDashboard.text = "$ramUsage%"

        Thread {
            val cpuUsage = CpuManager.getCpuUsage()
            val temp = ThermalManager.getCpuTemp()
            
            handler.post {
                if (_binding != null) {
                    binding.progressCpuCircular.setProgress(cpuUsage, true)
                    binding.tvCpuValueDashboard.text = "$cpuUsage%"

                    binding.progressTempCircular.setProgress(temp.toInt(), true)
                    binding.tvTempValueDashboard.text = "${temp.toInt()}°C"
                    
                    // Update overall status based on stats
                    updateOverallStatus(ramUsage, cpuUsage, temp)

                    // Simulated Ping and Battery Current for visual appeal
                    updateSimulatedStats()
                }
            }
        }.start()
    }

    private fun updateOverallStatus(ram: Int, cpu: Int, temp: Float) {
        val status: String
        val color: Int
        
        when {
            ram > 85 || cpu > 85 || temp > 45 -> {
                status = "CRITICAL"
                color = resources.getColor(R.color.accent_red)
            }
            ram > 60 || cpu > 60 || temp > 38 -> {
                status = "MODERATE"
                color = resources.getColor(R.color.accent_orange)
            }
            else -> {
                status = "EXCELLENT"
                color = resources.getColor(R.color.accent_green)
            }
        }
        
        binding.tvOverallStatus.text = status
        binding.tvOverallStatus.setTextColor(color)
    }

    private fun updateSimulatedStats() {
        val ping = Utils.getPing()
        binding.tvPingValue.text = "${ping}ms"
        binding.tvPingValue.setTextColor(if (ping < 40) resources.getColor(R.color.accent_green) else resources.getColor(R.color.accent_orange))
        
        val battery = ThermalManager.getBatteryCurrent(requireContext())
        binding.tvBatteryCurrent.text = "${battery}mA"
    }

    private fun performBoost() {
        binding.btnBoost.isEnabled = false
        binding.btnBoost.text = "OPTIMIZING..."
        
        Thread {
            RamManager.boostRam(requireContext(), isRooted)
            Thread.sleep(1500)
            handler.post {
                if (_binding != null) {
                    binding.btnBoost.isEnabled = true
                    binding.btnBoost.text = "BOOST"
                    Toast.makeText(requireContext(), "System Optimized!", Toast.LENGTH_SHORT).show()
                    updateStats()
                }
            }
        }.start()
    }

    private fun gameBoost() {
        Toast.makeText(requireContext(), "Activating Game Mode...", Toast.LENGTH_SHORT).show()
        Thread {
            RamManager.boostRam(requireContext(), isRooted)
            if (isRooted) {
                CpuManager.setGovernor("performance")
            }
            handler.post {
                Toast.makeText(requireContext(), "Game Mode Active!", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun showCpuControlDialog() {
        val governors = CpuManager.getAvailableGovernors()
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.Theme_UltraOptimizeX)
        builder.setTitle("Select CPU Governor")
        builder.setItems(governors.toTypedArray()) { _, which ->
            val selected = governors[which]
            CpuManager.setGovernor(selected)
            Toast.makeText(requireContext(), "Governor set to $selected", Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateRunnable)
        _binding = null
    }
}
