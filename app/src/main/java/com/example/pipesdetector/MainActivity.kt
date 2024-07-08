package com.example.pipesdetector

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL



class MainActivity : AppCompatActivity() {
    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val userLogin : EditText = findViewById(R.id.UserNameLogInScreen)
        val userPassword : EditText = findViewById(R.id.UserPasswordLogInScreen)
        val logInButton : Button = findViewById(R.id.LogInButton)
        val toRegistrationButton : Button = findViewById(R.id.ToRegInScreenButton)

        val intentForScanner = Intent(this, ScannerWindowActivity::class.java)
        val intentForRegistration = Intent(this, RegistrationActivity::class.java)

        logInButton.setOnClickListener {
            if(isFieldEmpty(userLogin)) {
                Toast.makeText(this, "Поле \"Логин\" должно быть заполнено", Toast.LENGTH_SHORT).show();}
            else if(isFieldEmpty(userPassword)){
                Toast.makeText(this, "Поле \"Пароль\" должно быть заполнено", Toast.LENGTH_SHORT).show();}
            else{
                GlobalScope.launch {
                    sendRequestToBackend(userLogin.text.toString(), userPassword.text.toString()) { result ->
                        if (result) {
                            startActivity(intentForScanner) }
                    }
                }
            }
        }

        toRegistrationButton.setOnClickListener {
            startActivity(intentForRegistration)
        }
    }

    private fun isFieldEmpty(text: EditText): Boolean {
        return text.text.toString().trim() == ""
    }

    private suspend fun sendRequestToBackend(login: String, password: String, onResult: (Boolean) -> Unit) {
        val jsonObject = JSONObject()
        jsonObject.put("name", login)
        jsonObject.put("password", password)
        val jsonObjectString = jsonObject.toString()
        val url = URL("http://192.168.0.177:8080/login")
        var flag = false
        withContext(Dispatchers.IO) {
            val httpURLConnection = url.openConnection() as HttpURLConnection

            httpURLConnection.requestMethod = "POST"
            httpURLConnection.setRequestProperty("Content-Type", "application/json")
            httpURLConnection.setRequestProperty("Accept", "application/json")
            httpURLConnection.doInput = true
            httpURLConnection.doOutput = true

            val outputStreamWriter = OutputStreamWriter(httpURLConnection.outputStream)
            outputStreamWriter.write(jsonObjectString)
            outputStreamWriter.flush()

            val responseCode = httpURLConnection.responseCode

            val inputStream = if (responseCode.toString().trim() == "302") { httpURLConnection.inputStream
            } else { httpURLConnection.errorStream }
            val response = inputStream.bufferedReader().use { it.readText() }
            val jsonResponse = JSONObject(response)
            if (responseCode.toString().trim() == "302") {
                flag = true
                withContext(Dispatchers.Main) {
                    val jwtToken = jsonResponse.getString("jwt-token")
                    val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    val editor = sharedPref.edit()
                    editor.putString("jwt-token", jwtToken)
                    editor.apply()
                    Log.d("JWT Token", jwtToken)
                }
            } else {
                flag = false
                val httpStatus = jsonResponse.getInt("httpStatus")
                val message = jsonResponse.getString("message")
                withContext(Dispatchers.Main) {
                    when (httpStatus) {
                        404 -> {
                            Log.e("Error", message)
                            Toast.makeText(this@MainActivity, "Error: $message", Toast.LENGTH_SHORT).show();
                        }
                        else -> {
                            Log.e("Error", "Unexpected error: $message")
                            Toast.makeText(this@MainActivity, "Error: $message", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        }
        onResult(flag)
    }
}