package com.example.hotelbookingapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    companion object {
        private const val SPLASH_DELAY_MS = 1800L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeIn.duration = 800
        findViewById<ImageView>(R.id.splashIcon).startAnimation(fadeIn)
        findViewById<TextView>(R.id.splashAppName).startAnimation(fadeIn)
        findViewById<TextView>(R.id.splashTagline).startAnimation(fadeIn)

        lifecycleScope.launch {
            delay(SPLASH_DELAY_MS)


            val target = if (FirebaseAuthManager.isLoggedIn) {
                MainActivity::class.java
            } else {
                LoginActivity::class.java
            }

            startActivity(Intent(this@SplashActivity, target))
            finish()
        }
    }
}