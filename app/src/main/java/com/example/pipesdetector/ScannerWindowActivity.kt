package com.example.pipesdetector

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
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
import com.example.pipesdetector.MainActivity.ServerIp
import kotlinx.coroutines.DelicateCoroutinesApi
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
    private lateinit var firstImageID: ImageView
    private lateinit var secondImageID: ImageView
    private lateinit var countOfPipesText : TextView
    private lateinit var info : TextView
    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner_window)

        getPermissions()

        val galleryButton: Button = findViewById(R.id.ButtonForGalery)
        info = findViewById(R.id.infoText)
        countOfPipesText = findViewById(R.id.NumberOfPipes)
        firstImageID = findViewById(R.id.imageBeforeNeuro)
        secondImageID = findViewById(R.id.imageAfterNeuro)
        val intentForLogin = Intent(this, MainActivity::class.java)

        galleryButton.setOnClickListener {
            GlobalScope.launch {
                checkSession { result ->
                    if (!result){
                        startActivity(intentForLogin)
                    }
                }
            }
            val alertDialog = AlertDialog.Builder(this)
            alertDialog.setTitle("Выберите источник")
            alertDialog.setItems(arrayOf("Камера", "Галерея")) { _, which ->
                when (which) {
                    1 -> {
                        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                            @Suppress("DEPRECATION")
                        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
                    }
                    0 -> {
                        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        @Suppress("DEPRECATION")
                        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
                    }
                }
            }
            alertDialog.show()
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

    private suspend fun checkSession(onResult: (Boolean) -> Unit) {
        var flag = false
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val jwtToken = sharedPref.getString("jwt-token", "")
        val url = URL("${ServerIp.IP}/signIn")
        withContext(Dispatchers.IO) {
            val httpURLConnection = url.openConnection() as HttpURLConnection
            httpURLConnection.requestMethod = "GET"
            httpURLConnection.setRequestProperty("Content-Type", "application/json")
            httpURLConnection.setRequestProperty("Accept", "application/json")
            httpURLConnection.setRequestProperty("Authorization", "Bearer $jwtToken")
            try{
                val responseCode = httpURLConnection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    flag = true
                    Log.d("JWT Token", "$jwtToken")
                } else {
                    Log.e("Error", "Failed to send request. Response code: $responseCode")
                }
            }catch (e: IOException){
                Log.e("Error", "Failed to send request: ${e.message}")
            } finally {
                httpURLConnection.disconnect()
            }
        }
        onResult(flag)
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SetTextI18n")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION") val photo = data?.extras?.get("data") as Bitmap
            firstImageID.setImageBitmap(photo)
            secondImageID.setImageResource(0)
            info.text = ""
            countOfPipesText.text = "Обработка"
            val imageString = encodeImageToString(photo)
            GlobalScope.launch {
                val (imageFromBackend, pipesCount) = sendRequestToBackend(imageString)
                val imageBitmapAfterDecode = decodeStringToImage(imageFromBackend)
                runOnUiThread {
                    secondImageID.setImageBitmap(imageBitmapAfterDecode)
                    countOfPipesText.text = "Число труб на фото: $pipesCount"
                }
            }
        } else if (requestCode == GALLERY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val selectedImageUri = data?.data
            val inputStream = selectedImageUri?.let { contentResolver.openInputStream(it) }
            val imageBitmap = BitmapFactory.decodeStream(inputStream)
            firstImageID.setImageBitmap(imageBitmap)
            secondImageID.setImageResource(0)
            info.text = ""
            countOfPipesText.text = "Обработка"
            val imageString = encodeImageToString(imageBitmap)
            GlobalScope.launch {
                val (imageFromBackend, pipesCount) = sendRequestToBackend(imageString)
                val imageBitmapAfterDecode = decodeStringToImage(imageFromBackend)
                runOnUiThread {
                    secondImageID.setImageBitmap(imageBitmapAfterDecode)
                    countOfPipesText.text = "Число труб на фото: $pipesCount"
                }
            }
        }
    }

    private fun encodeImageToString(imageBitmap: Bitmap): String {
        return try {
            val baos = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val imageBytes = baos.toByteArray()
            Base64.encodeToString(imageBytes, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            Log.e("encodeImageToString", "Error encoding image: ${e.message}", e)
            ""
        }
    }

    private fun decodeStringToImage(imageString: String): Bitmap? {
        return try {
            val imageBytes = Base64.decode(imageString, Base64.DEFAULT)
            val stream = ByteArrayInputStream(imageBytes)
            BitmapFactory.decodeStream(stream)
        } catch (e: IllegalArgumentException) {
            Log.e("decodeStringToImage", "Error decoding image: ${e.message}", e)
            null
        }
    }

    private suspend fun sendRequestToBackend(imageString: String) : Pair<String, Int> {
        return withContext(Dispatchers.IO) {
            var encodedImage = ""
            var objectsCount = 0
            val jsonObject = JSONObject()
            jsonObject.put("imgString", imageString)
            val jsonObjectString = jsonObject.toString()
            val url = URL("${ServerIp.IP}/countPipes")
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
