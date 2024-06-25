package com.example.pipesdetector

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
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

        // Реализация перехода в окно входа
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