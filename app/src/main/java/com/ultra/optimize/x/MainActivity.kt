package com.ultra.optimize.x

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.ultra.optimize.x.databinding.ActivityMainBinding
import com.ultra.optimize.x.utils.RootManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request root access on startup
        if (!RootManager.requestRoot()) {
            Toast.makeText(this, "Root access denied. Some features may not work.", Toast.LENGTH_LONG).show()
        }
    }
}
