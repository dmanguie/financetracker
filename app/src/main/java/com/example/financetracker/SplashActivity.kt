package com.example.financetracker

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // ── Fix 1: Use proper view types ──────────────────────────────
        val cardLogo  = findViewById<CardView>(R.id.cardLogo)
        val tvAppName = findViewById<View>(R.id.tvAppName)
        val tvTagline = findViewById<View>(R.id.tvTagline)
        val accentBar = findViewById<View>(R.id.accentBar)
        val tvVersion = findViewById<View>(R.id.tvVersion)

        // ── Fix 2: Block back press without deprecated onBackPressed ──
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing — prevent returning to splash
            }
        })

        // ── Animations ────────────────────────────────────────────────
        cardLogo.scaleX = 0.6f
        cardLogo.scaleY = 0.6f
        cardLogo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(500)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        tvAppName.translationY = 30f
        tvAppName.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(300)
            .setDuration(400)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        tvTagline.animate()
            .alpha(1f)
            .setStartDelay(500)
            .setDuration(400)
            .start()

        accentBar.animate()
            .alpha(1f)
            .setStartDelay(650)
            .setDuration(400)
            .start()

        tvVersion.animate()
            .alpha(0.6f)
            .setStartDelay(650)
            .setDuration(400)
            .start()

        // ── Navigate after 2 seconds ──────────────────────────────────
        cardLogo.postDelayed({ goToDashboard() }, 2000)
    }

    private fun goToDashboard() {
        startActivity(
            Intent(this, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        // ── Fix 3: Use ActivityOptions instead of deprecated overridePendingTransition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}