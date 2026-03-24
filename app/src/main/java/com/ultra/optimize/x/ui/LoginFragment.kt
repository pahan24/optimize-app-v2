package com.ultra.optimize.x.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.ultra.optimize.x.R
import com.ultra.optimize.x.databinding.FragmentLoginBinding
import com.ultra.optimize.x.utils.SettingsManager

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            showError("Google sign in failed: ${e.message}")
        }
    }

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

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isEmpty()) {
                binding.tilUsername.error = "Enter username"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                binding.tilPassword.error = "Enter password"
                return@setOnClickListener
            }

            // Hardcoded fallback for first-time setup or connection issues
            if (password == "123456") {
                SettingsManager.setLoggedIn(requireContext(), true)
                findNavController().navigate(R.id.action_login_to_dashboard)
                return@setOnClickListener
            }

            // Master Admin OTP (Temporary fix for Google Sign-In issues)
            if (password == "ADMIN_1234") {
                SettingsManager.setLoggedIn(requireContext(), true)
                SettingsManager.setAdmin(requireContext(), true)
                findNavController().navigate(R.id.action_login_to_dashboard)
                return@setOnClickListener
            }

            performLogin(password)
        }

        binding.btnAdminLogin.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        binding.btnAdminLogin.visibility = View.GONE
        binding.progressLogin.visibility = View.VISIBLE

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user?.email == "bpahan685@gmail.com") {
                        SettingsManager.setLoggedIn(requireContext(), true)
                        SettingsManager.setAdmin(requireContext(), true)
                        findNavController().navigate(R.id.action_login_to_dashboard)
                    } else {
                        auth.signOut()
                        showError("Access denied. Admin only.")
                    }
                } else {
                    showError("Authentication failed.")
                }
            }
    }

    private fun animateEntrance() {
        val views = listOf(
            binding.tvWelcome,
            binding.tvSubtitle,
            binding.tilUsername,
            binding.tilPassword,
            binding.btnLogin,
            binding.btnAdminLogin
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
                            .addOnFailureListener { e ->
                                showError("Login failed: ${e.message}")
                            }
                    } else {
                        showError("This OTP has already been used.")
                    }
                } else {
                    showError("Invalid OTP password.")
                }
            }
            .addOnFailureListener { e ->
                showError("Connection error: ${e.message}")
            }
    }

    private fun showError(message: String) {
        binding.btnLogin.visibility = View.VISIBLE
        binding.btnAdminLogin.visibility = View.VISIBLE
        binding.progressLogin.visibility = View.GONE
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
