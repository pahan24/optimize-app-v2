package com.ultra.optimize.x.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.ultra.optimize.x.R
import com.ultra.optimize.x.databinding.FragmentAdminBinding
import com.ultra.optimize.x.utils.Constants
import java.util.*

class AdminFragment : Fragment() {

    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance(Constants.FIRESTORE_DATABASE_ID)
    private lateinit var otpAdapter: OtpAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val context = context ?: return
        // Set Spannable Title
        val title = "SYSTEM\nADMINISTRATION"
        val spannable = android.text.SpannableString(title)
        val blueColor = androidx.core.content.ContextCompat.getColor(context, R.color.neon_blue)
        spannable.setSpan(android.text.style.ForegroundColorSpan(blueColor), 0, 6, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        
        if (_binding != null) {
            binding.tvAdminTitle.text = spannable

            setupRecyclerView()
            setupListeners()
            loadOtps()
            loadFeatureConfig()
        }
    }

    private fun loadFeatureConfig() {
        db.collection("app_config").document("features")
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    binding.switchEnableRoot.isChecked = doc.getBoolean("enable_root") ?: true
                    binding.switchEnableShizuku.isChecked = doc.getBoolean("enable_shizuku") ?: true
                    binding.switchEnableBypass.isChecked = doc.getBoolean("enable_bypass") ?: true
                }
            }
    }

    private fun setupRecyclerView() {
        otpAdapter = OtpAdapter(emptyList()) { otp ->
            deleteOtp(otp)
        }
        binding.rvOtps.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = otpAdapter
        }
    }

    private fun setupListeners() {
        binding.btnSaveOtp.setOnClickListener {
            val otp = binding.etOtpCode.text.toString().trim()
            val isAdmin = binding.cbIsAdmin.isChecked
            if (otp.isNotEmpty()) {
                saveOtp(otp, isAdmin)
            } else {
                Toast.makeText(context, "Please enter an OTP", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSaveAdmin.setOnClickListener {
            val email = binding.etAdminEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                saveAdminEmail(email)
            } else {
                Toast.makeText(context, "Please enter an email", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnPushUpdate.setOnClickListener {
            val versionCode = binding.etVersionCode.text.toString().trim().toLongOrNull() ?: 0L
            val versionName = binding.etVersionName.text.toString().trim()
            val updateUrl = binding.etUpdateUrl.text.toString().trim()
            val forceUpdate = binding.switchForceUpdate.isChecked

            if (versionCode > 0 && versionName.isNotEmpty() && updateUrl.isNotEmpty()) {
                pushUpdateInfo(versionCode, versionName, updateUrl, forceUpdate)
            } else {
                Toast.makeText(context, "Please fill all update fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSaveFeatures.setOnClickListener {
            saveFeatureConfig()
        }

        binding.btnSendBroadcast.setOnClickListener {
            val msg = binding.etBroadcastMsg.text.toString().trim()
            if (msg.isNotEmpty()) {
                sendBroadcast(msg)
            } else {
                Toast.makeText(context, "Please enter a message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveFeatureConfig() {
        val data = hashMapOf(
            "enable_root" to binding.switchEnableRoot.isChecked,
            "enable_shizuku" to binding.switchEnableShizuku.isChecked,
            "enable_bypass" to binding.switchEnableBypass.isChecked,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("app_config").document("features")
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(context, "Feature Config Saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendBroadcast(msg: String) {
        val data = hashMapOf(
            "message" to msg,
            "timestamp" to com.google.firebase.Timestamp.now(),
            "active" to true
        )

        db.collection("app_config").document("broadcast")
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(context, "Broadcast Sent Successfully", Toast.LENGTH_SHORT).show()
                binding.etBroadcastMsg.setText("")
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun pushUpdateInfo(code: Long, name: String, url: String, force: Boolean) {
        val data = hashMapOf(
            "latest_version_code" to code,
            "latest_version_name" to name,
            "update_url" to url,
            "force_auto_update" to force,
            "update_required" to force,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("app_config").document("version_info")
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(context, "Update Info Pushed Successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveAdminEmail(email: String) {
        val data = hashMapOf(
            "email" to email,
            "addedAt" to com.google.firebase.Timestamp.now()
        )
        db.collection("admins").document(email)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(context, "Admin Authorized: $email", Toast.LENGTH_SHORT).show()
                binding.etAdminEmail.setText("")
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveOtp(otp: String, isAdmin: Boolean) {
        val data = hashMapOf(
            "code" to otp,
            "isUsed" to false,
            "isAdmin" to isAdmin,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        
        db.collection("otps").document(otp)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(context, "OTP Saved Successfully", Toast.LENGTH_SHORT).show()
                binding.etOtpCode.setText("")
                loadOtps()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadOtps() {
        db.collection("otps")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(context, "Error loading OTPs", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val otps = snapshot.documents.map { doc ->
                        Otp(
                            code = doc.getString("code") ?: "",
                            isUsed = doc.getBoolean("isUsed") ?: false
                        )
                    }
                    otpAdapter.updateData(otps)
                }
            }
    }

    private fun deleteOtp(otp: Otp) {
        db.collection("otps").document(otp.code)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "OTP Deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error deleting OTP", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
