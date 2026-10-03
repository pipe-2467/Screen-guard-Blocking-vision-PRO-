package com.example.imagescreenguard

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: PrivacyOverlayView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundServiceNotification()

        val imageUriString = intent?.getStringExtra("IMAGE_URI")
        val opacity = intent?.getFloatExtra("OPACITY", 0.8f) ?: 0.8f
        val holeSize = intent?.getFloatExtra("HOLE_SIZE", 150f) ?: 150f

        if (overlayView == null) {
            setupOverlay(imageUriString, opacity, holeSize)
        } else {
            updateOverlay(imageUriString, opacity, holeSize)
        }

        return START_STICKY
    }

    private fun setupOverlay(imageUriString: String?, opacity: Float, holeSize: Float) {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // อนุญาตให้รับ Touch Event บนช่องมองได้
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        overlayView = PrivacyOverlayView(this).apply {
            imageOpacity = (opacity * 255).toInt()
            holeRadius = holeSize
            if (imageUriString != null) {
                setImageUri(Uri.parse(imageUriString))
            }
        }

        windowManager?.addView(overlayView, params)
    }

    private fun updateOverlay(imageUriString: String?, opacity: Float, holeSize: Float) {
        overlayView?.apply {
            imageOpacity = (opacity * 255).toInt()
            holeRadius = holeSize
            if (imageUriString != null) {
                setImageUri(Uri.parse(imageUriString))
            }
        }
    }

    private fun startForegroundServiceNotification() {
        val channelId = "screen_guard_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Screen Guard Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Privacy Screen Guard Active")
            .setContentText("โหมดบังสายตาแบบเจาะช่องมองกำลังทำงาน")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()

        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (overlayView != null) {
            windowManager?.removeView(overlayView)
            overlayView = null
        }
    }
}
