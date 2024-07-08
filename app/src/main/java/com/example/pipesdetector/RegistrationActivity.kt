package com.example.pipesdetector

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Email
import android.util.JsonToken
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.HttpRequest
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.HttpResponse
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.HttpClient
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.http.client.methods.RequestBuilder.post
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import khttp.post
import khttp.responses.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class RegistrationActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        val toLogInButton : Button = findViewById(R.id.ToLogInScreen)
        val RegInButton : Button = findViewById(R.id.RegInButton)
        val userEmail : EditText = findViewById(R.id.EmailAddress)
        val userLogin : EditText = findViewById(R.id.UserLoginRegistrationScreen)
        val userPassword : EditText = findViewById(R.id.UserPasswordRegistrationScreen)

        val intent1 = Intent(this, ScannerWindowActivity::class.java)

        toLogInButton.setOnClickListener {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
        }

        RegInButton.setOnClickListener {
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

        val url = URL("http://192.168.0.177:8080/registration")
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
            if (responseCode.toString() == "201") {
                flag = true
                val response = httpURLConnection.inputStream.bufferedReader()
                    .use { it.readText() }
                val jsonResponse = JSONObject(response)
                withContext(Dispatchers.Main) {
                    val jwtToken = jsonResponse.getString("jwt-token")
                    val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    val editor = sharedPref.edit()
                    editor.putString("jwt-token", jwtToken)
                    editor.apply()
                    Log.d("JWT Token", jwtToken)
                }
            } else {
                Log.e("HTTPURLCONNECTION_ERROR", responseCode.toString())
            }
        }
        onResult(flag)
    }
}