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
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()
    private var selectedRole: UserRole = UserRole.GUEST

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etName        = findViewById<TextInputEditText>(R.id.etRegFullName)
        val etEmail       = findViewById<TextInputEditText>(R.id.etRegEmail)
        val etPass        = findViewById<TextInputEditText>(R.id.etRegPassword)
        val etConfirm     = findViewById<TextInputEditText>(R.id.etRegConfirmPassword)
        val btnReg        = findViewById<Button>(R.id.btnRegister)
        val tvLogin       = findViewById<TextView>(R.id.tvGoToLogin)
        val progress      = findViewById<ProgressBar>(R.id.registerProgress)
        val roleToggle    = findViewById<MaterialButtonToggleGroup>(R.id.toggleGroupRole)

        // Default selection = Guest
        roleToggle.check(R.id.btnRoleGuest)
        roleToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedRole = if (checkedId == R.id.btnRoleHost) UserRole.HOST else UserRole.GUEST
            }
        }

        btnReg.setOnClickListener {
            viewModel.register(
                etName.text.toString(),
                etEmail.text.toString(),
                etPass.text.toString(),
                etConfirm.text.toString(),
                selectedRole
            )
        }

        tvLogin.setOnClickListener { finish() }

        lifecycleScope.launch {
            viewModel.authState.collect { state ->
                progress.visibility = if (state is AuthState.Loading) View.VISIBLE else View.GONE
                btnReg.isEnabled    = state !is AuthState.Loading
                if (state is AuthState.Success) {
                    startActivity(Intent(this@RegisterActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
            }
        }

        lifecycleScope.launch {
            viewModel.errorEvent.collect { msg ->
                Toast.makeText(this@RegisterActivity, msg, Toast.LENGTH_LONG).show()
            }
        }
    }
}