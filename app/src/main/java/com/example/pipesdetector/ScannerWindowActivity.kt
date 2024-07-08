package com.example.pipesdetector

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


class ScannerWindowActivity : AppCompatActivity() {
    private val CAMERA_REQUEST_CODE = 1
    private val CAMERA_PERMISSION_CODE = 2
    private val GALLERY_PERMISSION_CODE = 3
    private val GALLERY_REQUEST_CODE = 4
    lateinit var firstImageID: ImageView
    lateinit var secondImageID: ImageView
    lateinit var countOfPipesText : TextView
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner_window)

        getPermissions()


        val cameraButton: Button = findViewById(R.id.ButtonForCamera)
        val galleryButton: Button = findViewById(R.id.ButtonForGalery)
        countOfPipesText = findViewById(R.id.NumberOfPipes)
        firstImageID = findViewById(R.id.imageBeforeNeuro)
        secondImageID = findViewById(R.id.imageAfterNeuro)

        cameraButton.setOnClickListener() {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
        }

        galleryButton.setOnClickListener() {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
        }
    }

    private fun getPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                CAMERA_PERMISSION_CODE
            )
            return false
        }
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                GALLERY_PERMISSION_CODE
            )
            return false
        }
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val photo = data?.extras?.get("data") as Bitmap
            firstImageID.setImageBitmap(photo)
            val imageString = encodeImageToString(photo)
            GlobalScope.launch {
                val (imageFromBackend, pipesCount) = sendRequestToBackend(imageString)
                val imageBitmapAfterDecode = decodeStringToImage(imageFromBackend)
                runOnUiThread {
                    secondImageID.setImageBitmap(imageBitmapAfterDecode)
                    countOfPipesText.setText("Число труб на фото: $pipesCount")
                }
            }
        } else if (requestCode == GALLERY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val selectedImageUri = data?.data
            val inputStream = selectedImageUri?.let { contentResolver.openInputStream(it) }
            val imageBitmap = BitmapFactory.decodeStream(inputStream)
            firstImageID.setImageBitmap(imageBitmap)
            val imageString = encodeImageToString(imageBitmap)
            GlobalScope.launch {
                val (imageFromBackend, pipesCount) = sendRequestToBackend(imageString)
                val imageBitmapAfterDecode = decodeStringToImage(imageFromBackend)
                runOnUiThread {
                    secondImageID.setImageBitmap(imageBitmapAfterDecode)
                    countOfPipesText.setText("Число труб на фото: $pipesCount")
                }
            }
        }
    }



    private fun encodeImageToString(imageBitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val imageBytes = baos.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.DEFAULT)
    }

    private fun decodeStringToImage(imageString: String): Bitmap {
        val imageBytes = Base64.decode(imageString, Base64.DEFAULT)
        val stream = ByteArrayInputStream(imageBytes)
        return BitmapFactory.decodeStream(stream)
    }


    private suspend fun sendRequestToBackend(imageString: String) : Pair<String, Int> {
        return withContext(Dispatchers.IO) {
            var encodedImage = ""
            var objectsCount = 0
            val jsonObject = JSONObject()
            jsonObject.put("imgString", imageString)
            val jsonObjectString = jsonObject.toString()
            val url = URL("http://192.168.0.177:8080/countPipes")
            val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val jwtToken = sharedPref.getString("jwt-token", "")
            (url.openConnection() as? HttpURLConnection)?.run {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $jwtToken")
                doOutput = true
                try {
                    outputStream.use { it.write(jsonObjectString.toByteArray()) }

                    val responseCode = responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        inputStream.bufferedReader().use { reader ->
                            val jsonResponse = JSONObject(reader.readText())
                            encodedImage = jsonResponse.getString("imgString")
                            objectsCount = jsonResponse.getInt("quantityPipes")
                        }
                    } else {
                        Log.e("Error", "Failed to send request. Response code: $responseCode")
                    }
                } catch (e: IOException) {
                    Log.e("Error", "Failed to send request: ${e.message}")
                } finally {
                    disconnect()
                }
            } ?: run {
                Log.e("Error", "Connection not established")
            }
            Pair(encodedImage, objectsCount)
        }
    }
}
