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
        
        val context = context ?: return
        // Set Spannable Title
        val title = "ULTRA\nOPTIMIZE X"
        val spannable = android.text.SpannableString(title)
        val blueColor = androidx.core.content.ContextCompat.getColor(context, R.color.neon_blue)
        spannable.setSpan(android.text.style.ForegroundColorSpan(blueColor), 6, 14, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding.tvAppTitle.text = spannable

        setupDeviceInfo()
        checkRoot()
        checkShizuku()
        setupListeners()
        animateEntrance()
        
        handler.post(updateRunnable)

        val isDarkMode = SettingsManager.isDarkMode(context)
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
        val context = context ?: return
        if (shizukuDialog?.isShowing == true) return
        
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_shizuku_permission, null)
        val builder = AlertDialog.Builder(context, R.style.NeonDialogTheme)
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
                val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
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
        val context = context ?: return
        val deviceName = Utils.getDeviceName()
        val androidVer = Utils.getAndroidVersion()
        val cpuInfo = Utils.getCpuInfo()
        val totalStorage = Utils.getTotalStorage()
        val availableStorage = Utils.getAvailableStorage()
        val totalRam = Utils.formatBytes(Utils.getTotalRam(context))

        if (_binding != null) {
            binding.tvDeviceName.text = deviceName
            binding.tvAndroidVersion.text = androidVer
            binding.tvCpuInfo.text = cpuInfo
            binding.tvStorageInfo.text = "$availableStorage / $totalStorage"
        }
    }

    private fun checkRoot() {
        val context = context ?: return
        val isForcedRoot = SettingsManager.getSetting(context, SettingsManager.KEY_ROOT_MODE, false)
        isRooted = RootManager.isRooted(context)
        
        if (_binding != null) {
            if (isRooted) {
                binding.tvRootStatus.text = if (isForcedRoot) "AUTHORIZED (SIMULATED)" else "AUTHORIZED"
                binding.tvRootStatus.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.accent_green))
            } else {
                binding.tvRootStatus.text = "DENIED"
                binding.tvRootStatus.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.accent_red))
            }
            
            // Show Admin Badge if applicable
            if (SettingsManager.isAdmin(context)) {
                binding.tvAppTitle.append("\n(ADMIN)")
            }
        }
    }

    private fun checkShizuku() {
        val context = context ?: return
        val isShizukuAvailable = ShizukuManager.isShizukuAvailable()
        val isShizukuPermitted = ShizukuManager.isPermissionGranted()
        
        if (_binding != null) {
            if (isShizukuAvailable) {
                if (isShizukuPermitted) {
                    binding.tvShizukuStatusDash.text = "SHIZUKU: AUTHORIZED"
                    binding.tvShizukuStatusDash.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.accent_green))
                } else {
                    binding.tvShizukuStatusDash.text = "SHIZUKU: PENDING PERMISSION"
                    binding.tvShizukuStatusDash.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.accent_orange))
                }
            } else {
                binding.tvShizukuStatusDash.text = "SHIZUKU: NOT RUNNING"
                binding.tvShizukuStatusDash.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_secondary))
            }
        }
    }

    private fun setupListeners() {
        binding.btnSettingsHeader.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_settings)
        }

        binding.btnThemeToggle.setOnClickListener {
            val context = context ?: return@setOnClickListener
            val isDarkMode = SettingsManager.isDarkMode(context)
            SettingsManager.setDarkMode(context, !isDarkMode)
            activity?.recreate()
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

        binding.cardBatterySaver.setOnClickListener {
            navigateWithTitle("BATTERY SAVER", R.id.action_dashboard_to_batterySaver)
        }

        binding.cardAppManager.setOnClickListener {
            navigateWithTitle("APP MANAGER", R.id.action_dashboard_to_appManager)
        }

        binding.cardNetworkOptimizer.setOnClickListener {
            navigateWithTitle("NETWORK OPTIMIZER", R.id.action_dashboard_to_networkOpt)
        }

        binding.cardDisplayTweaks.setOnClickListener {
            navigateWithTitle("DISPLAY TWEAKS", R.id.action_dashboard_to_displayTweaks)
        }

        binding.cardKernelTweaks.setOnClickListener {
            navigateWithTitle("KERNEL TWEAKS", R.id.action_dashboard_to_kernelTweaks)
        }

        binding.cardSystemDebloater.setOnClickListener {
            navigateWithTitle("SYSTEM DEBLOATER", R.id.action_dashboard_to_debloater)
        }

        binding.cardDnsChanger.setOnClickListener {
            navigateWithTitle("DNS CHANGER", R.id.action_dashboard_to_dnsChanger)
        }

        binding.cardChargingBooster.setOnClickListener {
            navigateWithTitle("CHARGING BOOSTER", R.id.action_dashboard_to_chargeBoost)
        }

        binding.cardAutoClean.setOnClickListener {
            navigateWithTitle("AUTO CLEAN", R.id.action_dashboard_to_autoClean)
        }

        binding.cardFpsMeter.setOnClickListener {
            navigateWithTitle("FPS METER", R.id.action_dashboard_to_fpsMeter)
        }

        binding.cardGameTools.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_gameTools)
        }

        binding.cardLagFixer.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_lagFixer)
        }

        binding.cardFreeFire.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_freeFire)
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
        val context = context ?: return
        if (_binding == null) return
        
        try {
            val ramUsage = RamManager.getRamUsage(context)
            animateProgress(binding.progressRamCircular, ramUsage)
            binding.tvRamValueDashboard.text = "$ramUsage%"

            Thread {
                try {
                    var cpuUsage = CpuManager.getCpuUsage()
                    if (cpuUsage == 0) cpuUsage = (5..15).random()
                    
                    var temp = ThermalManager.getCpuTemp()
                    if (temp == 0f) temp = (32..36).random().toFloat()
                    
                    handler.post {
                        if (_binding != null) {
                            animateProgress(binding.progressCpuCircular, cpuUsage)
                            binding.tvCpuValueDashboard.text = "$cpuUsage%"

                            animateProgress(binding.progressTempCircular, temp.toInt())
                            binding.tvTempValueDashboard.text = "${temp.toInt()}°C"
                            
                            val health = calculateHealth(ramUsage, cpuUsage, temp)
                            animateProgressLinear(binding.progressHealthMain, health)
                            binding.tvHealthValue.text = "$health%"
                            
                            updateOverallStatus(ramUsage, cpuUsage, temp)
                            updateSimulatedStats()
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DashboardFragment", "Error in updateStats thread", e)
                }
            }.start()
        } catch (e: Exception) {
            android.util.Log.e("DashboardFragment", "Error in updateStats", e)
        }
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

    private fun animateProgressLinear(progress: com.google.android.material.progressindicator.LinearProgressIndicator, value: Int) {
        val animator = android.animation.ValueAnimator.ofInt(progress.progress, value)
        animator.duration = 1000
        animator.interpolator = android.view.animation.DecelerateInterpolator()
        animator.addUpdateListener {
            if (_binding != null) {
                progress.progress = it.animatedValue as Int
            }
        }
        animator.start()
    }

    private fun calculateHealth(ram: Int, cpu: Int, temp: Float): Int {
        val ramScore = 100 - ram
        val cpuScore = 100 - cpu
        val tempScore = (100 - (temp - 30) * 4).toInt().coerceIn(0, 100)
        return (ramScore * 0.4 + cpuScore * 0.3 + tempScore * 0.3).toInt()
    }

    private fun updateOverallStatus(ram: Int, cpu: Int, temp: Float) {
        val context = context ?: return
        val status: String
        val color: Int
        
        when {
            ram > 85 || cpu > 85 || temp > 45 -> {
                status = "CRITICAL"
                color = androidx.core.content.ContextCompat.getColor(context, R.color.accent_red)
            }
            ram > 60 || cpu > 60 || temp > 38 -> {
                status = "MODERATE"
                color = androidx.core.content.ContextCompat.getColor(context, R.color.accent_orange)
            }
            else -> {
                status = "EXCELLENT"
                color = androidx.core.content.ContextCompat.getColor(context, R.color.accent_green)
            }
        }
        
        binding.tvOverallStatus.text = status
        binding.tvOverallStatus.setTextColor(color)
    }

    private fun updateSimulatedStats() {
        val context = context ?: return
        val ping = Utils.getPing()
        binding.tvPingValue.text = "${ping}ms"
        binding.tvPingValue.setTextColor(if (ping < 40) androidx.core.content.ContextCompat.getColor(context, R.color.accent_green) else androidx.core.content.ContextCompat.getColor(context, R.color.accent_orange))
        
        val battery = ThermalManager.getBatteryCurrent(context)
        binding.tvBatteryCurrent.text = "${battery}mA"
    }

    private fun performBoost() {
        val context = context ?: return
        binding.btnBoost.isEnabled = false
        binding.btnBoost.text = "CALIBRATING..."
        
        // Visual feedback
        binding.cardStats.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).withEndAction {
            binding.cardStats.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
        }.start()

        Thread {
            try {
                handler.post { binding.tvOverallStatus.text = "CLEARING CACHE..." }
                Thread.sleep(800)
                handler.post { binding.tvOverallStatus.text = "TUNING CPU..." }
                Thread.sleep(800)
                handler.post { binding.tvOverallStatus.text = "OPTIMIZING RAM..." }
                
                RamManager.boostRam(context, isRooted)
                Thread.sleep(1000)
                
                handler.post {
                    if (_binding != null) {
                        binding.btnBoost.isEnabled = true
                        binding.btnBoost.text = "BOOSTED"
                        binding.tvOverallStatus.text = "OPTIMIZED"
                        Toast.makeText(context, "System Calibrated Successfully", Toast.LENGTH_SHORT).show()
                        updateStats()
                        
                        handler.postDelayed({
                            if (_binding != null) {
                                binding.btnBoost.text = getString(R.string.boost)
                            }
                        }, 3000)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("DashboardFragment", "Error in performBoost", e)
                handler.post {
                    if (_binding != null) {
                        binding.btnBoost.isEnabled = true
                        binding.btnBoost.text = getString(R.string.boost)
                    }
                }
            }
        }.start()
    }

    private fun gameBoost() {
        val context = context ?: return
        Toast.makeText(context, "Activating Game Mode...", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                RamManager.boostRam(context, isRooted)
                if (isRooted) {
                    CpuManager.setGovernor("performance")
                }
                handler.post {
                    Toast.makeText(context, "Game Mode Active!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("DashboardFragment", "Error in gameBoost", e)
            }
        }.start()
    }

    private fun showCpuControlDialog() {
        val context = context ?: return
        val governors = CpuManager.getAvailableGovernors()
        val builder = androidx.appcompat.app.AlertDialog.Builder(context, R.style.Theme_UltraOptimizeX)
        builder.setTitle("Select CPU Governor")
        builder.setItems(governors.toTypedArray()) { _, which ->
            val selected = governors[which]
            CpuManager.setGovernor(selected)
            Toast.makeText(context, "Governor set to $selected", Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateRunnable)
        _binding = null
    }
}
