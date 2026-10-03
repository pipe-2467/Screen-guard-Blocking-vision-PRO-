package com.example.imagescreenguard

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var selectedImageUri: Uri? = null
    private var currentOpacity: Float = 0.8f
    private var currentHoleSize: Float = 150f

    private lateinit var previewImage: ImageView

    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            selectedImageUri = it
            previewImage.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkOverlayPermission()

        previewImage = findViewById(R.id.previewImage)
        val btnSelectImage = findViewById<Button>(R.id.btnSelectImage)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)
        val seekBarOpacity = findViewById<SeekBar>(R.id.seekBarOpacity)
        val seekBarHoleSize = findViewById<SeekBar>(R.id.seekBarHoleSize)

        btnSelectImage.setOnClickListener {
            selectImageLauncher.launch("image/*")
        }

        seekBarOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentOpacity = progress / 100f
                previewImage.alpha = currentOpacity

                if (selectedImageUri != null) {
                    startGuardService()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarHoleSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentHoleSize = progress.toFloat().coerceAtLeast(50f)
                if (selectedImageUri != null) {
                    startGuardService()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnStart.setOnClickListener {
            if (selectedImageUri == null) {
                Toast.makeText(this, "กรุณาเลือกรูปภาพก่อน", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startGuardService()
        }

        btnStop.setOnClickListener {
            val intent = Intent(this, OverlayService::class.java)
            stopService(intent)
        }
    }

    private fun startGuardService() {
        val intent = Intent(this, OverlayService::class.java).apply {
            putExtra("IMAGE_URI", selectedImageUri.toString())
            putExtra("OPACITY", currentOpacity)
            putExtra("HOLE_SIZE", currentHoleSize)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }
}
