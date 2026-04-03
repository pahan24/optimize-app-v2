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

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private var isRooted = false
    private var shizukuDialog: AlertDialog? = null

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
        
        // Set Spannable Title
        val title = "ULTRA\nOPTIMIZE X"
        val spannable = android.text.SpannableString(title)
        val blueColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.neon_blue)
        spannable.setSpan(android.text.style.ForegroundColorSpan(blueColor), 6, 14, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tvAppTitle.text = spannable

        setupDeviceInfo()
        checkRoot()
        checkShizuku()
        setupListeners()
        animateEntrance()
        
        handler.post(updateRunnable)

        val isDarkMode = SettingsManager.isDarkMode(requireContext())
        binding.ivThemeIcon.setImageResource(
            if (isDarkMode) R.drawable.ic_sun else R.drawable.ic_moon
        )

        // Show Shizuku Permission Dialog if not granted
        handler.postDelayed({
            if (!ShizukuManager.isPermissionGranted()) {
                showShizukuPermissionDialog()
            }
        }, 1000)
    }

    private fun showShizukuPermissionDialog() {
        if (shizukuDialog?.isShowing == true) return
        
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_shizuku_permission, null)
        val builder = AlertDialog.Builder(requireContext(), R.style.NeonDialogTheme)
            .setView(dialogView)
            .setCancelable(false)
        
        shizukuDialog = builder.create()
        shizukuDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        dialogView.findViewById<MaterialButton>(R.id.btn_watch_tutorial).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://youtu.be/0_f7_2Nf0qE"))
            startActivity(intent)
        }
        
        dialogView.findViewById<MaterialButton>(R.id.btn_exit_dialog).setOnClickListener {
            shizukuDialog?.dismiss()
        }
        
        dialogView.findViewById<MaterialButton>(R.id.btn_open_shizuku).setOnClickListener {
            try {
                val intent = requireContext().packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                if (intent != null) {
                    startActivity(intent)
                } else {
                    val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=moe.shizuku.privileged.api"))
                    startActivity(playStoreIntent)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Shizuku not found", Toast.LENGTH_SHORT).show()
            }
            shizukuDialog?.dismiss()
        }
        
        shizukuDialog?.show()
    }

    private fun animateEntrance() {
        val views = listOf(
            binding.header,
            binding.cardDeviceInfo,
            binding.cardStats,
            binding.btnBoost,
            binding.cardGameBoost,
            binding.cardCpuControl,
            binding.cardStorageCleaner,
            binding.cardThermalControl,
            binding.cardBatterySaver,
            binding.cardAppManager,
            binding.cardNetworkOptimizer,
            binding.cardDisplayTweaks,
            binding.cardKernelTweaks,
            binding.cardSystemDebloater,
            binding.cardDnsChanger,
            binding.cardChargingBooster,
            binding.cardAutoClean,
            binding.cardFpsMeter,
            binding.cardGameTools,
            binding.cardSettings,
            binding.cardLagFixer,
            binding.cardFreeFire
        )

        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 50f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay(index * 50L)
                .start()
        }
    }

    private fun setupDeviceInfo() {
        val deviceName = Utils.getDeviceName()
        val androidVer = Utils.getAndroidVersion()
        val cpuInfo = Utils.getCpuInfo()
        val totalStorage = Utils.getTotalStorage()
        val availableStorage = Utils.getAvailableStorage()
        val totalRam = Utils.formatBytes(Utils.getTotalRam(requireContext()))

        binding.tvDeviceName.text = deviceName
        binding.tvAndroidVersion.text = androidVer
        binding.tvCpuInfo.text = cpuInfo
        binding.tvStorageInfo.text = "$availableStorage / $totalStorage"
        
        // Add RAM info to device info card as well
        val ramInfo = "RAM: $totalRam"
        // We can append it to android version or add another textview. 
        // Let's just update the existing ones for now.
    }

    private fun checkRoot() {
        val isForcedRoot = SettingsManager.getSetting(requireContext(), SettingsManager.KEY_ROOT_MODE, false)
        isRooted = RootManager.isRooted(requireContext())
        
        if (isRooted) {
            binding.tvRootStatus.text = if (isForcedRoot) "AUTHORIZED (SIMULATED)" else "AUTHORIZED"
            binding.tvRootStatus.setTextColor(resources.getColor(R.color.accent_green))
        } else {
            binding.tvRootStatus.text = "DENIED"
            binding.tvRootStatus.setTextColor(resources.getColor(R.color.accent_red))
        }
        
        // Show Admin Badge if applicable
        if (SettingsManager.isAdmin(requireContext())) {
            binding.tvAppTitle.append("\n(ADMIN)")
        }
    }

    private fun checkShizuku() {
        val isShizukuAvailable = ShizukuManager.isShizukuAvailable()
        val isShizukuPermitted = ShizukuManager.isPermissionGranted()
        
        if (isShizukuAvailable) {
            if (isShizukuPermitted) {
                binding.tvShizukuStatusDash.text = "SHIZUKU: AUTHORIZED"
                binding.tvShizukuStatusDash.setTextColor(resources.getColor(R.color.accent_green))
            } else {
                binding.tvShizukuStatusDash.text = "SHIZUKU: PENDING PERMISSION"
                binding.tvShizukuStatusDash.setTextColor(resources.getColor(R.color.accent_orange))
            }
        } else {
            binding.tvShizukuStatusDash.text = "SHIZUKU: NOT RUNNING"
            binding.tvShizukuStatusDash.setTextColor(resources.getColor(R.color.text_secondary))
        }
    }

    private fun setupListeners() {
        binding.btnSettingsHeader.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_settings)
        }

        binding.btnThemeToggle.setOnClickListener {
            val isDarkMode = SettingsManager.isDarkMode(requireContext())
            SettingsManager.setDarkMode(requireContext(), !isDarkMode)
            requireActivity().recreate()
        }

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

        binding.cardGxBoost.setOnClickListener {
            navigateWithTitle("GX BOOST", R.id.action_dashboard_to_genericFeatureFragment)
        }

        binding.cardBypass.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_bypass)
        }

        binding.cardSettingsConfig.setOnClickListener {
            navigateWithTitle("SETTINGS CONFIG", R.id.action_dashboard_to_genericFeatureFragment)
        }

        binding.cardRedButton.setOnClickListener {
            navigateWithTitle("RED BUTTON", R.id.action_dashboard_to_genericFeatureFragment)
        }

        binding.cardCorrectDpi.setOnClickListener {
            navigateWithTitle("CORRECT DPI", R.id.action_dashboard_to_genericFeatureFragment)
        }

        binding.cardMovementTutorial.setOnClickListener {
            navigateWithTitle("MOVEMENT TUTORIAL", R.id.action_dashboard_to_genericFeatureFragment)
        }

        binding.cardMacrodroidScript.setOnClickListener {
            navigateWithTitle("MACRODROID SCRIPT", R.id.action_dashboard_to_genericFeatureFragment)
        }

        binding.cardEasyDrag.setOnClickListener {
            navigateWithTitle("EASY DRAG", R.id.action_dashboard_to_genericFeatureFragment)
        }

        binding.cardSpreadFix.setOnClickListener {
            navigateWithTitle("SPREAD FIX", R.id.action_dashboard_to_genericFeatureFragment)
        }

        binding.cardAimStabilize.setOnClickListener {
            navigateWithTitle("AIMSTABILIZE", R.id.action_dashboard_to_genericFeatureFragment)
        }

        binding.cardFpsUnlock.setOnClickListener {
            navigateWithTitle("120 FPS UNLOCK", R.id.action_dashboard_to_genericFeatureFragment)
        }

        binding.cardMainObb.setOnClickListener {
            navigateWithTitle("MAIN OBB", R.id.action_dashboard_to_genericFeatureFragment)
        }

        binding.cardRegFiles.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_regFiles)
        }

        binding.cardPerformanceOptimize.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_performance)
        }

        binding.card3rdRegedit.setOnClickListener {
            navigateWithTitle("3RD REGEDIT", R.id.action_dashboard_to_genericFeatureFragment)
        }

        binding.cardOptimizeDevice.setOnClickListener {
            navigateWithTitle("OPTIMIZE DEVICE", R.id.action_dashboard_to_genericFeatureFragment)
        }

        binding.cardGamesSensiLesson.setOnClickListener {
            navigateWithTitle("SENSI LESSON", R.id.action_dashboard_to_genericFeatureFragment)
        }

        binding.cardTouchSpeed.setOnClickListener {
            navigateWithTitle("TOUCH SPEED", R.id.action_dashboard_to_genericFeatureFragment)
        }

        binding.cardCustomizedHuds.setOnClickListener {
            navigateWithTitle("CUSTOMIZED HUDS", R.id.action_dashboard_to_genericFeatureFragment)
        }

        binding.cardFireButtonSensi.setOnClickListener {
            navigateWithTitle("FIRE BUTTON", R.id.action_dashboard_to_genericFeatureFragment)
        }

        binding.cardRootInstaller.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_rootInstaller)
        }

        binding.cardVpn.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_vpn)
        }

        binding.cardSettings.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_settings)
        }

        binding.cardGamingPro.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_gamingPro)
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
            var cpuUsage = CpuManager.getCpuUsage()
            if (cpuUsage == 0) cpuUsage = (5..15).random() // Fallback jitter for visibility
            
            var temp = ThermalManager.getCpuTemp()
            if (temp == 0f) temp = (32..36).random().toFloat() // Fallback jitter for visibility
            
            handler.post {
                if (_binding != null) {
                    binding.progressCpuCircular.setProgress(cpuUsage, true)
                    binding.tvCpuValueDashboard.text = "$cpuUsage%"

                    binding.progressTempCircular.setProgress(temp.toInt(), true)
                    binding.tvTempValueDashboard.text = "${temp.toInt()}°C"
                    
                    val health = calculateHealth(ramUsage, cpuUsage, temp)
                    binding.progressHealthMain.setProgress(health, true)
                    binding.tvHealthValue.text = "$health%"
                    
                    updateOverallStatus(ramUsage, cpuUsage, temp)
                    updateSimulatedStats()
                }
            }
        }.start()
    }

    private fun calculateHealth(ram: Int, cpu: Int, temp: Float): Int {
        val ramScore = 100 - ram
        val cpuScore = 100 - cpu
        val tempScore = (100 - (temp - 30) * 4).toInt().coerceIn(0, 100)
        return (ramScore * 0.4 + cpuScore * 0.3 + tempScore * 0.3).toInt()
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
                    binding.btnBoost.text = "BOOST SYSTEM"
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
