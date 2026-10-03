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
import android.widget.ImageView
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: ImageView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundServiceNotification()

        val imageUriString = intent?.getStringExtra("IMAGE_URI")
        val opacity = intent?.getFloatExtra("OPACITY", 0.5f) ?: 0.5f

        if (overlayView == null) {
            setupOverlay(imageUriString, opacity)
        } else {
            updateOverlay(imageUriString, opacity)
        }

        return START_STICKY
    }

    private fun setupOverlay(imageUriString: String?, opacity: Float) {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        overlayView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            alpha = opacity
            if (imageUriString != null) {
                try {
                    setImageURI(Uri.parse(imageUriString))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        windowManager?.addView(overlayView, params)
    }

    private fun updateOverlay(imageUriString: String?, opacity: Float) {
        overlayView?.apply {
            alpha = opacity
            if (imageUriString != null) {
                try {
                    setImageURI(Uri.parse(imageUriString))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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
            .setContentTitle("Image Screen Guard Active")
            .setContentText("กำลังแสดงรูปภาพบังสายตาบนหน้าจอ")
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
