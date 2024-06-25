package com.example.pipesdetector

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val userData : EditText = findViewById(R.id.UserNameLogInScreen)
        val userPassword : EditText = findViewById(R.id.UserPasswordLogInScreen)
        val logInButton : Button = findViewById(R.id.LogInButton)
        val toRegistrationButton : Button = findViewById(R.id.ToRegInScreenButton)

        logInButton.setOnClickListener {

        }

        toRegistrationButton.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
        }
    }
}