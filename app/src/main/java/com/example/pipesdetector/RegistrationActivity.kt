package com.example.pipesdetector

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Email
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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

        toLogInButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        RegInButton.setOnClickListener {
            val intent = Intent(this, ScannerWindowActivity::class.java)
            startActivity(intent)
        }
    }
}