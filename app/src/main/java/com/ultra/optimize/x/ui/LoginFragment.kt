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
import com.ultra.optimize.x.utils.Constants
import com.ultra.optimize.x.utils.SettingsManager

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance(Constants.FIRESTORE_DATABASE_ID)
    private val auth = FirebaseAuth.getInstance()
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            val statusCode = e.statusCode
            val errorMessage = when (statusCode) {
                7 -> "Network error. Please check your internet connection."
                10 -> "Developer error. Ensure SHA-1 is registered in Firebase console and client ID is correct."
                12500 -> "Sign-in failed. Play Services might be outdated or misconfigured."
                12501 -> "Sign-in cancelled by user."
                else -> "Google sign in failed (Code $statusCode): ${e.message}"
            }
            showError(errorMessage)
            android.util.Log.e("LoginFragment", "Google Sign-In Error: $statusCode", e)
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

        val context = context ?: return
        // Set Spannable Title
        val title = "ULTRA\nOPTIMIZE X"
        val spannable = android.text.SpannableString(title)
        val blueColor = androidx.core.content.ContextCompat.getColor(context, R.color.neon_blue)
        spannable.setSpan(android.text.style.ForegroundColorSpan(blueColor), 6, 14, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        
        if (_binding != null) {
            binding.tvAppTitle.text = spannable

            // Check if already logged in
            if (SettingsManager.isLoggedIn(context)) {
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
                    binding.tilUsername.error = "Enter Username"
                    return@setOnClickListener
                }

                if (password.length < 6) {
                    binding.tilPassword.error = "Enter valid OTP"
                    return@setOnClickListener
                }

                performLogin(password)
            }

            binding.btnAdminLogin.setOnClickListener {
                val clientId = getString(R.string.default_web_client_id)
                if (clientId.contains("dummy")) {
                    Toast.makeText(context, "Google Sign-In is not configured. Please update the Web Client ID in strings.xml from your Firebase Console.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
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
                    val email = user?.email ?: ""
                    
                    // Check if this email is in the admins collection
                    db.collection("admins").document(email).get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists() || email == "bpahan685@gmail.com" || email == "sasmikarashmika49@gmail.com") {
                                SettingsManager.setLoggedIn(requireContext(), true)
                                SettingsManager.setAdmin(requireContext(), true)
                                findNavController().navigate(R.id.action_login_to_dashboard)
                            } else {
                                auth.signOut()
                                showError("Access denied. Admin only.")
                            }
                        }
                        .addOnFailureListener {
                            // Fallback to hardcoded for safety if firestore fails
                            if (email == "bpahan685@gmail.com" || email == "sasmikarashmika49@gmail.com") {
                                SettingsManager.setLoggedIn(requireContext(), true)
                                SettingsManager.setAdmin(requireContext(), true)
                                findNavController().navigate(R.id.action_login_to_dashboard)
                            } else {
                                auth.signOut()
                                showError("Authentication error.")
                            }
                        }
                } else {
                    showError("Authentication failed.")
                }
            }
    }

    private fun animateEntrance() {
        val views = listOf(
            binding.cvLogo,
            binding.tvAppTitle,
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
                    val isAdmin = document.getBoolean("isAdmin") ?: false
                    
                    if (!isUsed) {
                        // Mark as used immediately
                        db.collection("otps").document(password)
                            .update("isUsed", true)
                            .addOnSuccessListener {
                                SettingsManager.setLoggedIn(requireContext(), true)
                                SettingsManager.setAdmin(requireContext(), isAdmin)
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
