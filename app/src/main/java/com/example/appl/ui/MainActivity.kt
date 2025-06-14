package com.example.appl.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.example.appl.R
import com.example.appl.utils.FileUtils
import java.io.File

class MainActivity : AppCompatActivity() {
    private val directoryPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { handleDirectorySelection(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize saved directory
        FileUtils.initializeDirectory(this)
        
        // Check if we already have a directory set
        if (FileUtils.getDataDirectory() != null) {
            startHomeActivity()
            finish()
            return
        }

        findViewById<Button>(R.id.btnSelect).setOnClickListener {
            checkAndRequestPermissions()
        }
    }

    private fun checkAndRequestPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("TanggaltoEat needs storage permission to store recipe data")
                    .setPositiveButton("Settings") { _, _ ->
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                return
            }
        }
        directoryPicker.launch(null)
    }

    private fun handleDirectorySelection(uri: Uri) {
        val directory = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val docFile = DocumentFile.fromTreeUri(this, uri)
            val path = docFile?.uri?.path?.let { uriPath ->
                val segments = uriPath.split(":")
                if (segments.size > 1) {
                    "${Environment.getExternalStorageDirectory()}/${segments[1]}"
                } else {
                    "${Environment.getExternalStorageDirectory()}/$uriPath"
                }
            }
            path?.let { File(it) }
        } else {
            val path = uri.path?.let { File(it) }
            path
        }

        directory?.let {
            val tanggaltoEatDir = File(it, "TanggaltoEat/Data")
            FileUtils.setDataDirectory(tanggaltoEatDir.absolutePath, this)
            startHomeActivity()
            finish()
        } ?: run {
            Toast.makeText(this, "Failed to access directory", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startHomeActivity() {
        startActivity(Intent(this, HomeActivity::class.java))
    }
} 