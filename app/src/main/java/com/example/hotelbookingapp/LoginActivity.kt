package com.example.hotelbookingapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        if (viewModel.isLoggedIn()) {
            goToMain(); return
        }

        setContentView(R.layout.activity_login)

        val etEmail    = findViewById<TextInputEditText>(R.id.etLoginEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etLoginPassword)
        val btnLogin   = findViewById<Button>(R.id.btnLogin)
        val btnGoReg   = findViewById<TextView>(R.id.tvGoToRegister)
        val progress   = findViewById<ProgressBar>(R.id.loginProgress)

        btnLogin.setOnClickListener {
            viewModel.login(
                etEmail.text.toString(),
                etPassword.text.toString()
            )
        }

        btnGoReg.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        lifecycleScope.launch {
            viewModel.authState.collect { state ->
                progress.visibility = if (state is AuthState.Loading) View.VISIBLE else View.GONE
                btnLogin.isEnabled  = state !is AuthState.Loading
                if (state is AuthState.Success) goToMain()
            }
        }

        lifecycleScope.launch {
            viewModel.errorEvent.collect { msg ->
                Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}