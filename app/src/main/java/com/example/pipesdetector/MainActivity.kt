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

        val userLogin : EditText = findViewById(R.id.UserNameLogInScreen)  // Логин для входа
        val userPassword : EditText = findViewById(R.id.UserPasswordLogInScreen) // Пароль для входа
        val logInButton : Button = findViewById(R.id.LogInButton) // Кнопка входа
        val toRegistrationButton : Button = findViewById(R.id.ToRegInScreenButton) // Кнопка перехода в окно регистрации

        logInButton.setOnClickListener {
            // Миша или Родион добавьте сюда проверку через ваш клиент-сервер
            // что такой пользователь существует
        }

        // Реализация перехода в окно регистрации
        toRegistrationButton.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
        }
    }
}