package com.ultra.optimize.x.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.ultra.optimize.x.databinding.FragmentAdminBinding
import java.util.*

class AdminFragment : Fragment() {

    private var _binding: FragmentAdminBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

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
        
        setupListeners()
        loadSessions()
    }

    private fun setupListeners() {
        binding.btnSaveOtp.setOnClickListener {
            val otp = binding.etOtpCode.text.toString().trim()
            if (otp.isNotEmpty()) {
                saveOtp(otp)
            } else {
                Toast.makeText(context, "Please enter an OTP", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveOtp(otp: String) {
        val data = hashMapOf(
            "code" to otp,
            "isUsed" to false,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        
        db.collection("otps").document(otp)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(context, "OTP Saved Successfully", Toast.LENGTH_SHORT).show()
                binding.etOtpCode.setText("")
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadSessions() {
        // Simple session loading logic could be added here
        // For now, we just show the OTP generation
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
