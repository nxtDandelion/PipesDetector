package com.example.pipesdetector

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ScannerWindowActivity : AppCompatActivity() {
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner_window)
        registerPermission()
        checkPermission()
        val cameraButton : Button = findViewById(R.id.ButtonForCamera)

        cameraButton.setOnClickListener(){
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, pic_id)
        }
    }

    private fun checkPermission(){
        when{
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED -> { }
            else->{
                permissionLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

    private fun registerPermission(){
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){
            Toast.makeText(this, "Разрешение получено", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode:Int, resultCode:Int, data:Intent?){
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == pic_id){
            val photo = data!!.extras!!["data"] as Bitmap
        }
    }

    companion object{
        private const val pic_id = 123
    }
}