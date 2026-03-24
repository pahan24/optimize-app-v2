package com.ultra.optimize.x.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.ultra.optimize.x.R
import com.ultra.optimize.x.databinding.FragmentLoginBinding
import com.ultra.optimize.x.utils.SettingsManager

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if already logged in
        if (SettingsManager.isLoggedIn(requireContext())) {
            findNavController().navigate(R.id.action_login_to_dashboard)
            return
        }

        animateEntrance()

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username != "ultrax") {
                binding.tilUsername.error = "Invalid username"
                return
            }

            if (password.isEmpty()) {
                binding.tilPassword.error = "Enter password"
                return
            }

            performLogin(password)
        }
    }

    private fun animateEntrance() {
        val views = listOf(
            binding.tvWelcome,
            binding.tvSubtitle,
            binding.tilUsername,
            binding.tilPassword,
            binding.btnLogin
        )

        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 30f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(index * 100L)
                .start()
        }
    }

    private fun performLogin(password: String) {
        binding.btnLogin.visibility = View.GONE
        binding.progressLogin.visibility = View.VISIBLE

        db.collection("otps").document(password).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val isUsed = document.getBoolean("isUsed") ?: true
                    if (!isUsed) {
                        // Mark as used immediately
                        db.collection("otps").document(password)
                            .update("isUsed", true)
                            .addOnSuccessListener {
                                SettingsManager.setLoggedIn(requireContext(), true)
                                findNavController().navigate(R.id.action_login_to_dashboard)
                            }
                            .addOnFailureListener {
                                showError("Login failed. Please try again.")
                            }
                    } else {
                        showError("This OTP has already been used.")
                    }
                } else {
                    showError("Invalid OTP password.")
                }
            }
            .addOnFailureListener {
                showError("Connection error. Check your internet.")
            }
    }

    private fun showError(message: String) {
        binding.btnLogin.visibility = View.VISIBLE
        binding.progressLogin.visibility = View.GONE
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
