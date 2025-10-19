package com.ariete.advancednotes.activities

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.ariete.advancednotes.databinding.ActivitySplashScreenBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /**
         * `lifecycleScope.launch` function launches a coroutine
         * within an Activity lifecycle scope for 700 milliseconds
         * before a transition between SplashScreenActivity and MainActivity
         */

        lifecycleScope.launch {
            delay(700)

            val intent = Intent(
                baseContext,
                MainActivity::class.java
            )
            startActivity(intent)
            finish()
        }
    }
}