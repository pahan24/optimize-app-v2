package com.ultra.optimize.x

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.ultra.optimize.x.databinding.ActivityMainBinding
import com.ultra.optimize.x.utils.RootManager
import com.ultra.optimize.x.utils.UpdateManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check for updates on startup
        UpdateManager(this).checkForUpdates()

        // Request root access on startup in background
        Thread {
            if (!RootManager.requestRoot()) {
                runOnUiThread {
                    Toast.makeText(this, "Root access denied. Some features may not work.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
}
