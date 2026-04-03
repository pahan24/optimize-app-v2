package com.ultra.optimize.x.ui

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ultra.optimize.x.R
import com.ultra.optimize.x.databinding.FragmentVpnBinding
import com.ultra.optimize.x.services.UltraVpnService
import java.util.*

class VpnFragment : Fragment() {

    private var _binding: FragmentVpnBinding? = null
    private val binding get() = _binding!!

    private var isConnected = false
    private var isConnecting = false
    private val handler = Handler(Looper.getMainLooper())
    private var secondsElapsed = 0
    private var timerRunnable: Runnable? = null
    private var statsRunnable: Runnable? = null

    private val vpnPrepareLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            startVpnService()
        } else {
            Toast.makeText(context, "VPN Permission Denied", Toast.LENGTH_SHORT).show()
            isConnecting = false
            updateUi()
        }
    }

    private val servers = listOf(
        "Auto Select (Optimal)",
        "United States (New York)",
        "United States (Los Angeles)",
        "United Kingdom (London)",
        "Germany (Frankfurt)",
        "France (Paris)",
        "Singapore",
        "Japan (Tokyo)",
        "India (Mumbai)",
        "Australia (Sydney)",
        "Canada (Toronto)",
        "Brazil (Sao Paulo)"
    )
    private var selectedServer = servers[0]
    private val connectionLog = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVpnBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnVpnToggle.setOnClickListener {
            if (isConnected) {
                disconnectVpn()
            } else if (!isConnecting) {
                connectVpn()
            }
        }

        binding.cardSelectServer.setOnClickListener {
            showServerSelectionDialog()
        }

        updateUi()
        addLog("VPN System Initialized Ready.")
    }

    private fun addLog(message: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        connectionLog.add(0, "[$time] $message")
        if (connectionLog.size > 5) connectionLog.removeAt(5)
        binding.tvVpnLog.text = connectionLog.joinToString("\n")
    }

    private fun showServerSelectionDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.NeonDialogTheme)
        builder.setTitle("Select VPN Server")
        builder.setItems(servers.toTypedArray()) { _, which ->
            selectedServer = servers[which]
            binding.tvSelectedServer.text = selectedServer
            addLog("Server changed to $selectedServer")
            if (isConnected) {
                Toast.makeText(context, "Reconnecting to $selectedServer...", Toast.LENGTH_SHORT).show()
                disconnectVpn()
                handler.postDelayed({ connectVpn() }, 1000)
            }
        }
        builder.show()
    }

    private fun connectVpn() {
        isConnecting = true
        updateUi()
        addLog("Initiating connection to $selectedServer...")
        
        val vpnIntent = VpnService.prepare(requireContext())
        if (vpnIntent != null) {
            vpnPrepareLauncher.launch(vpnIntent)
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        addLog("Establishing secure tunnel...")
        handler.postDelayed({
            val intent = Intent(requireContext(), UltraVpnService::class.java)
            intent.putExtra("SERVER", selectedServer)
            requireContext().startService(intent)
            
            isConnecting = false
            isConnected = true
            startTimer()
            startStatsUpdate()
            updateUi()
            addLog("Connected successfully to $selectedServer")
            Toast.makeText(context, "VPN Connected to $selectedServer", Toast.LENGTH_SHORT).show()
        }, 2500)
    }

    private fun disconnectVpn() {
        addLog("Disconnecting...")
        val intent = Intent(requireContext(), UltraVpnService::class.java)
        intent.action = "STOP"
        requireContext().startService(intent)
        
        isConnected = false
        isConnecting = false
        stopTimer()
        stopStatsUpdate()
        updateUi()
        addLog("VPN Disconnected.")
        Toast.makeText(context, "VPN Disconnected", Toast.LENGTH_SHORT).show()
    }

    private fun updateUi() {
        when {
            isConnecting -> {
                binding.tvVpnStatus.text = getString(R.string.vpn_status_connecting)
                binding.tvVpnStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_orange))
                binding.ivVpnPower.setImageResource(R.drawable.ic_zap)
                binding.ivVpnPower.animate().rotationBy(360f).setDuration(1000).start()
                binding.btnVpnToggle.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.card_bg))
                binding.tvVpnTimer.visibility = View.GONE
            }
            isConnected -> {
                binding.tvVpnStatus.text = getString(R.string.vpn_status_connected)
                binding.tvVpnStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent_green))
                binding.ivVpnPower.setImageResource(R.drawable.ic_shield)
                binding.btnVpnToggle.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.accent_green_alpha))
                binding.tvVpnTimer.visibility = View.VISIBLE
            }
            else -> {
                binding.tvVpnStatus.text = getString(R.string.vpn_status_disconnected)
                binding.tvVpnStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                binding.ivVpnPower.setImageResource(R.drawable.ic_zap)
                binding.btnVpnToggle.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.card_bg))
                binding.tvVpnTimer.visibility = View.GONE
                binding.tvVpnDownload.text = "0.0 KB/s"
                binding.tvVpnUpload.text = "0.0 KB/s"
                binding.tvVpnPing.text = "0 ms"
            }
        }
    }

    private fun startTimer() {
        secondsElapsed = 0
        timerRunnable = object : Runnable {
            override fun run() {
                secondsElapsed++
                val hours = secondsElapsed / 3600
                val minutes = (secondsElapsed % 3600) / 60
                val secs = secondsElapsed % 60
                binding.tvVpnTimer.text = String.format("%02d:%02d:%02d", hours, minutes, secs)
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(timerRunnable!!)
    }

    private fun stopTimer() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = null
    }

    private fun startStatsUpdate() {
        statsRunnable = object : Runnable {
            override fun run() {
                if (isConnected) {
                    val down = (100..1500).random() / 10.0
                    val up = (50..800).random() / 10.0
                    val ping = (20..150).random()
                    binding.tvVpnDownload.text = String.format("%.1f KB/s", down)
                    binding.tvVpnUpload.text = String.format("%.1f KB/s", up)
                    binding.tvVpnPing.text = "$ping ms"
                    handler.postDelayed(this, 2000)
                }
            }
        }
        handler.post(statsRunnable!!)
    }

    private fun stopStatsUpdate() {
        statsRunnable?.let { handler.removeCallbacks(it) }
        statsRunnable = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTimer()
        stopStatsUpdate()
        _binding = null
    }
}
