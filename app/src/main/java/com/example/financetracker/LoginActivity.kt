package com.example.financetracker

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class LoginActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvError: TextView
    private lateinit var btnTogglePassword: TextView
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = AuthManager(this)

        if (authManager.isLoggedIn()) {
            goToDashboard()
            return
        }

        setContentView(R.layout.activity_login)
        bindViews()
        setupListeners()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAffinity()
            }
        })
    }

    private fun bindViews() {
        etUsername        = findViewById(R.id.etUsername)
        etPassword        = findViewById(R.id.etPassword)
        btnLogin          = findViewById(R.id.btnLogin)
        tvError           = findViewById(R.id.tvError)
        btnTogglePassword = findViewById(R.id.btnTogglePassword)
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener { attemptLogin() }

        etPassword.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                        event.action == KeyEvent.ACTION_DOWN)
            ) {
                attemptLogin()
                true
            } else false
        }

        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etPassword.transformationMethod = if (isPasswordVisible) {
                btnTogglePassword.text = "🙈"
                HideReturnsTransformationMethod.getInstance()
            } else {
                btnTogglePassword.text = "👁"
                PasswordTransformationMethod.getInstance()
            }
            etPassword.setSelection(etPassword.text.length)
        }

        val clearError = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) { hideError() }
            override fun afterTextChanged(s: Editable?) {}
        }
        etUsername.addTextChangedListener(clearError)
        etPassword.addTextChangedListener(clearError)
    }

    private fun attemptLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (username.isEmpty()) {
            etUsername.error = "Username is required"
            etUsername.requestFocus()
            return
        }
        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            etPassword.requestFocus()
            return
        }

        if (authManager.checkCredentials(username, password)) {
            authManager.saveLoginState(true)
            goToDashboard()
        } else {
            showError()
        }
    }

    private fun goToDashboard() {
        startActivity(
            Intent(this, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun showError() {
        tvError.visibility = View.VISIBLE
        val shake = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.shake)
        tvError.startAnimation(shake)
    }

    private fun hideError() {
        tvError.visibility = View.GONE
    }
}