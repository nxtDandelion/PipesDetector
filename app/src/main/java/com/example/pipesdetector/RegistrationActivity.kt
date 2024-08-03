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
import com.example.pipesdetector.MainActivity.ServerIp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class RegistrationActivity : AppCompatActivity() {
    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        val toLogInButton : Button = findViewById(R.id.ToLogInScreen)
        val regInButton : Button = findViewById(R.id.RegInButton)
        val userEmail : EditText = findViewById(R.id.EmailAddress)
        val userLogin : EditText = findViewById(R.id.UserLoginRegistrationScreen)
        val userPassword : EditText = findViewById(R.id.UserPasswordRegistrationScreen)

        val intent1 = Intent(this, ScannerWindowActivity::class.java)

        toLogInButton.setOnClickListener {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
        }

        regInButton.setOnClickListener {
            if(isFieldEmpty(userEmail)) {
                Toast.makeText(this, "Поле \"Почта\" должно быть заполнено", Toast.LENGTH_SHORT).show();}
            else if(isFieldEmpty(userLogin)){
                Toast.makeText(this, "Поле \"Логин\" должно быть заполнено", Toast.LENGTH_SHORT).show();}
            else if(isFieldEmpty(userPassword)){
                Toast.makeText(this, "Поле \"Пароль\" должно быть заполнено", Toast.LENGTH_SHORT).show();}
            else{
                GlobalScope.launch {
                sendRequestToBackend(userLogin.text.toString(), userPassword.text.toString(), userEmail.text.toString()) { result ->
                    if (result) {
                        startActivity(intent1) }
                }}
            }
        }
    }

    private fun isFieldEmpty(text: EditText): Boolean {
        return text.text.toString().trim() == ""
    }

    private suspend fun sendRequestToBackend(login : String, password : String, email : String, onResult: (Boolean) -> Unit){
        val jsonObject = JSONObject()
        jsonObject.put("name", login)
        jsonObject.put("password", password)
        jsonObject.put("email", email)
        val jsonObjectString = jsonObject.toString()
        var flag = false
        val url = URL("${ServerIp.IP}/registration")
        withContext(Dispatchers.IO){
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
            val response: String = inputStream.bufferedReader()
                .use { it.readText() }
            if (responseCode.toString() == "201") {
                flag = true
                val jsonResponse = JSONObject(response)
                withContext(Dispatchers.Main) {
                    val jwtToken = jsonResponse.getString("jwt-token")
                    val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    val editor = sharedPref.edit()
                    editor.putString("jwt-token", jwtToken)
                    editor.apply()
                    Log.d("JWT Token", jwtToken)
                }
            }
            else if (responseCode.toString() == "400"){
                val jsonResponse = JSONObject(response)
                val violationsArray = jsonResponse.getJSONArray("violations")
                for (i in 0 until violationsArray.length()) {
                    val violation = violationsArray.getJSONObject(i)
                    val fieldName = violation.getString("fieldName")
                    val message = violation.getString("message")
                    Log.e("Validation Error", "Field: $fieldName, Message: $message")
                    runOnUiThread { Toast.makeText(this@RegistrationActivity,
                        "$fieldName: $message",
                            Toast.LENGTH_LONG).show()
                    }
                }
            }
            else if (responseCode.toString() == "409") {
                val jsonResponse = JSONObject(response)
                withContext(Dispatchers.Main) {
                    val message = jsonResponse.getString("message")
                    Log.e("Validation Error", "Message: $message")
                    runOnUiThread { Toast.makeText(this@RegistrationActivity,
                        message,
                        Toast.LENGTH_LONG).show()
                    }
                }
            }
            else
            {
                Log.e("ERROR", "Unexpected Error")
                runOnUiThread { Toast.makeText(this@RegistrationActivity,
                    "Sorry, Unexpected Error",
                    Toast.LENGTH_LONG).show()
                }
            }
        }
        onResult(flag)
    }
}