// app/src/main/java/com/example/financetracker/LoginActivity.kt
package com.example.financetracker

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class LoginActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager

    // ── Login form views ───────────────────────────────────────────────────
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnCreateAccount: MaterialButton
    private lateinit var tvError: TextView
    private lateinit var btnTogglePassword: TextView

    private var isPasswordVisible = false

    // ── Lifecycle ──────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = AuthManager(this)

        // If already signed in (Firebase session persists), skip to Dashboard
        if (authManager.isLoggedIn()) {
            goToDashboard()
            return
        }

        setContentView(R.layout.activity_login)
        bindViews()
        setupListeners()

        // Disable the system back button on the login screen
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { finishAffinity() }
        })
    }

    // ── View binding ───────────────────────────────────────────────────────

    private fun bindViews() {
        // In the XML the id is etUsername — Firebase Auth uses email but
        // we keep the XML id so we don't have to touch other files.
        etEmail           = findViewById(R.id.etUsername)
        etPassword        = findViewById(R.id.etPassword)
        btnLogin          = findViewById(R.id.btnLogin)
        btnCreateAccount  = findViewById(R.id.btnCreateAccount)
        tvError           = findViewById(R.id.tvError)
        btnTogglePassword = findViewById(R.id.btnTogglePassword)
    }

    // ── Listeners ──────────────────────────────────────────────────────────

    private fun setupListeners() {

        // Login button
        btnLogin.setOnClickListener { attemptLogin() }

        // "Create Account" button → shows the registration dialog
        btnCreateAccount.setOnClickListener { showCreateAccountDialog() }

        // Allow "Done" on the keyboard to trigger login
        etPassword.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                        event.action == KeyEvent.ACTION_DOWN)
            ) {
                attemptLogin(); true
            } else false
        }

        // Toggle password visibility
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

        // Clear the error banner while the user is typing
        val clearError = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) { hideError() }
            override fun afterTextChanged(s: Editable?) {}
        }
        etEmail.addTextChangedListener(clearError)
        etPassword.addTextChangedListener(clearError)
    }

    // ── Login flow ─────────────────────────────────────────────────────────

    private fun attemptLogin() {
        val email    = etEmail.text.toString().trim()
        val password = etPassword.text.toString()

        // Client-side validation first
        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            etEmail.requestFocus()
            return
        }
        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            etPassword.requestFocus()
            return
        }

        setFormEnabled(false)   // disable buttons while waiting for Firebase

        authManager.login(
            email    = email,
            password = password,
            onSuccess = {
                setFormEnabled(true)
                goToDashboard()
            },
            onError = { message ->
                setFormEnabled(true)
                showError(message)
            }
        )
    }

    // ── Create Account dialog ──────────────────────────────────────────────

    /**
     * Shows a dialog with four fields:
     *   Username  |  Email  |  Password  |  Confirm Password
     *
     * On submit: calls Firebase Auth createAccount + saves username to Firestore.
     * The positive button is wired manually so we can prevent auto-dismiss on error.
     */
    private fun showCreateAccountDialog() {
        // ── Build the form programmatically ───────────────────────────────
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 40, 64, 24)
        }

        fun makeField(hint: String, inputType: Int): EditText {
            return EditText(this).apply {
                this.hint = hint
                this.inputType = inputType
                setTextColor(0xFFF0F2FF.toInt())
                setHintTextColor(0xFF555A70.toInt())
                textSize = 15f
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(0, 0, 0, 20) }
                layoutParams = lp
            }
        }

        val etName = makeField(
            "Username  (e.g. Juan)",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
        )
        val etDialogEmail = makeField(
            "Email  (e.g. juan@email.com)",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        )
        val etDialogPass = makeField(
            "Password  (min 6 characters)",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        ).also { it.transformationMethod = PasswordTransformationMethod.getInstance() }

        val etConfirm = makeField(
            "Confirm Password",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        ).also { it.transformationMethod = PasswordTransformationMethod.getInstance() }

        dialogLayout.addView(etName)
        dialogLayout.addView(etDialogEmail)
        dialogLayout.addView(etDialogPass)
        dialogLayout.addView(etConfirm)

        // ── Build the AlertDialog ──────────────────────────────────────────
        val dialog = AlertDialog.Builder(this)
            .setTitle("✨  Create Account")
            .setMessage("Fill in the details below to register.")
            .setView(dialogLayout)
            .setPositiveButton("Create Account", null)   // null → we handle click manually
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        // Override the positive button click so the dialog stays open on error
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {

            val username = etName.text.toString().trim()
            val email    = etDialogEmail.text.toString().trim()
            val password = etDialogPass.text.toString()
            val confirm  = etConfirm.text.toString()

            // ── Validation ─────────────────────────────────────────────────
            when {
                username.isEmpty() -> {
                    etName.error = "Username is required"
                    etName.requestFocus()
                    return@setOnClickListener
                }
                email.isEmpty() -> {
                    etDialogEmail.error = "Email is required"
                    etDialogEmail.requestFocus()
                    return@setOnClickListener
                }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    etDialogEmail.error = "Enter a valid email"
                    etDialogEmail.requestFocus()
                    return@setOnClickListener
                }
                password.length < 6 -> {
                    etDialogPass.error = "Minimum 6 characters"
                    etDialogPass.requestFocus()
                    return@setOnClickListener
                }
                password != confirm -> {
                    etConfirm.error = "Passwords do not match"
                    etConfirm.requestFocus()
                    return@setOnClickListener
                }
            }

            // ── Disable buttons and call Firebase ──────────────────────────
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = false
            dialog.setMessage("Creating your account…")

            authManager.createAccount(
                username = username,
                email    = email,
                password = password,
                onSuccess = {
                    dialog.dismiss()
                    Toast.makeText(
                        this,
                        "🎉 Account created! Please log in.",
                        Toast.LENGTH_LONG
                    ).show()
                    // Pre-fill the email field so the user just enters the password
                    etEmail.setText(email)
                    etPassword.requestFocus()
                },
                onError = { message ->
                    // Re-enable buttons and show what went wrong
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).isEnabled = true
                    dialog.setMessage("Fill in the details below to register.")
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // ── Navigation ─────────────────────────────────────────────────────────

    private fun goToDashboard() {
        startActivity(
            Intent(this, DashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /** Shows the red error banner with a shake animation. */
    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
        val shake = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.shake)
        tvError.startAnimation(shake)
    }

    private fun hideError() {
        tvError.visibility = View.GONE
    }

    /** Enables / disables both buttons while a network call is in flight. */
    private fun setFormEnabled(enabled: Boolean) {
        btnLogin.isEnabled         = enabled
        btnCreateAccount.isEnabled = enabled
        btnLogin.text              = if (enabled) "Login" else "Logging in…"
    }
}