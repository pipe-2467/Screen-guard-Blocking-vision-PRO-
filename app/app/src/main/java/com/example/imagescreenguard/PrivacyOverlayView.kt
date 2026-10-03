package com.example.imagescreenguard

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View

class PrivacyOverlayView(context: Context) : View(context) {

    private var bitmap: Bitmap? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private var touchX = -1f
    private var touchY = -1f
    var holeRadius = 150f // ขนาดวงกลมช่องมอง
        set(value) {
            field = value
            invalidate()
        }

    var imageOpacity = 220 // ความทึบแสงของรูปภาพบังสายตา (0-255)
        set(value) {
            field = value
            invalidate()
        }

    init {
        // บังคับใช้ Hardware Acceleration Layer เพื่อรองรับ PorterDuff CLEAR
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun setImageUri(uri: Uri) {
        try {
            val rawBitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            val displayMetrics = resources.displayMetrics
            bitmap = Bitmap.createScaledBitmap(
                rawBitmap,
                displayMetrics.widthPixels,
                displayMetrics.heightPixels,
                true
            )
            invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val currentBitmap = bitmap ?: return

        // 1. วาดรูปภาพบังสายตาแบบทึบแสง
        paint.alpha = imageOpacity
        canvas.drawBitmap(currentBitmap, 0f, 0f, paint)

        // 2. เจาะช่องวงกลมใสตรงตำแหน่งที่แตะนิ้ว
        if (touchX >= 0 && touchY >= 0) {
            canvas.drawCircle(touchX, touchY, holeRadius, clearPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                touchX = event.x
                touchY = event.y
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchX = -1f
                touchY = -1f
                invalidate()
            }
        }
        return true
    }
}
