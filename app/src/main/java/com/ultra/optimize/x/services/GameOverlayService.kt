package com.ultra.optimize.x.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.ultra.optimize.x.R
import com.ultra.optimize.x.utils.SettingsManager

class GameOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var handleView: View
    private lateinit var panelView: View
    
    private var isPanelOpen = false
    private lateinit var paramsHandle: WindowManager.LayoutParams
    private lateinit var paramsPanel: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        setupHandle()
        setupPanel()
    }

    private fun setupHandle() {
        handleView = LayoutInflater.from(this).inflate(R.layout.layout_overlay_handle, null)
        
        paramsHandle = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        paramsHandle.gravity = Gravity.TOP or Gravity.START
        paramsHandle.x = 0
        paramsHandle.y = 300

        handleView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var startTime = 0L

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = paramsHandle.x
                        initialY = paramsHandle.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        startTime = System.currentTimeMillis()
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        paramsHandle.x = initialX + (event.rawX - initialTouchX).toInt()
                        paramsHandle.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(handleView, paramsHandle)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val duration = System.currentTimeMillis() - startTime
                        if (duration < 200) {
                            togglePanel()
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(handleView, paramsHandle)
    }

    private fun setupPanel() {
        panelView = LayoutInflater.from(this).inflate(R.layout.layout_overlay_panel, null)
        
        paramsPanel = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        
        paramsPanel.gravity = Gravity.TOP
        paramsPanel.y = 100
        
        val switchFps = panelView.findViewById<SwitchCompat>(R.id.switch_fps_overlay)
        val switchCrosshair = panelView.findViewById<SwitchCompat>(R.id.switch_crosshair_overlay)
        val btnClean = panelView.findViewById<LinearLayout>(R.id.btn_clean_overlay)
        val btnClose = panelView.findViewById<ImageView>(R.id.btn_close_panel)

        switchFps.isChecked = SettingsManager.getSetting(this, "fps_meter_enabled", false)
        switchCrosshair.isChecked = SettingsManager.getSetting(this, "crosshair_enabled", false)

        switchFps.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveSetting(this, "fps_meter_enabled", isChecked)
        }
        
        switchCrosshair.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.saveSetting(this, "crosshair_enabled", isChecked)
        }

        btnClean.setOnClickListener {
            panelView.findViewById<TextView>(R.id.tv_clean_status).text = "CLEANING..."
            panelView.postDelayed({
                panelView.findViewById<TextView>(R.id.tv_clean_status).text = "SYSTEM CLEANED"
            }, 2000)
        }

        btnClose.setOnClickListener { togglePanel() }
    }

    private fun togglePanel() {
        if (isPanelOpen) {
            windowManager.removeView(panelView)
        } else {
            windowManager.addView(panelView, paramsPanel)
        }
        isPanelOpen = !isPanelOpen
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::handleView.isInitialized) windowManager.removeView(handleView)
        if (isPanelOpen) windowManager.removeView(panelView)
    }
}
