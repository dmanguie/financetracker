// app/src/main/java/com/example/financetracker/AuthManager.kt
package com.example.financetracker

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Handles all authentication operations using Firebase Auth.
 *
 * - Login / logout / session check via FirebaseAuth
 * - User profile (username) stored in Firestore under "users/{uid}"
 *
 * All callbacks run on the main thread (Firebase default behaviour).
 */
class AuthManager(context: Context) {

    // We keep context as a parameter so the constructor signature stays the
    // same — existing callers (SplashActivity, DashboardActivity) don't break.
    @Suppress("UNUSED_PARAMETER")
    constructor(context: Context, unused: Unit) : this(context)

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    // ── Session ────────────────────────────────────────────────────────────

    /**
     * Returns true if a user is currently signed in.
     * Firebase Auth persists the session automatically across app restarts.
     */
    fun isLoggedIn(): Boolean = auth.currentUser != null

    /** Signs the current user out. */
    fun logout() = auth.signOut()

    /** Returns the uid of the signed-in user, or null. */
    fun currentUid(): String? = auth.currentUser?.uid

    // ── Login ──────────────────────────────────────────────────────────────

    /**
     * Signs in with [email] and [password].
     *
     * @param onSuccess called when login succeeds
     * @param onError   called with a human-readable message when login fails
     */
    fun login(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception ->
                val message = when (exception) {
                    is FirebaseAuthInvalidUserException         ->
                        "No account found with this email."
                    is FirebaseAuthInvalidCredentialsException ->
                        "Incorrect email or password."
                    else ->
                        exception.message ?: "Login failed. Please try again."
                }
                onError(message)
            }
    }

    // ── Create Account ─────────────────────────────────────────────────────

    /**
     * Creates a new Firebase Auth user and saves the [username] to Firestore.
     *
     * Firestore path: users/{uid}/  →  { username, email }
     *
     * @param onSuccess called when both Auth and Firestore writes succeed
     * @param onError   called with a human-readable message on any failure
     */
    fun createAccount(
        username: String,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    onError("Account created but could not retrieve user ID.")
                    return@addOnSuccessListener
                }

                // Save the username (and email) in Firestore so we can
                // display it in the app later if needed.
                val profile = hashMapOf(
                    "username" to username.trim(),
                    "email"    to email.trim()
                )
                db.collection("users").document(uid)
                    .set(profile)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        // Auth user was created — sign them out so they
                        // must retry (profile is incomplete without username).
                        auth.signOut()
                        onError("Account created but profile save failed: ${e.message}")
                    }
            }
            .addOnFailureListener { exception ->
                val message = when (exception) {
                    is FirebaseAuthUserCollisionException  ->
                        "This email is already registered. Please log in."
                    is FirebaseAuthWeakPasswordException   ->
                        "Password is too weak. Use at least 6 characters."
                    is FirebaseAuthInvalidCredentialsException ->
                        "Invalid email address format."
                    else ->
                        exception.message ?: "Account creation failed. Please try again."
                }
                onError(message)
            }
    }

    // ── Profile ────────────────────────────────────────────────────────────

    /**
     * Fetches the display username for the current user from Firestore.
     * Calls [onResult] with the username string, or "User" as a fallback.
     */
    fun getUsername(onResult: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: run { onResult("User"); return }
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                onResult(doc.getString("username") ?: "User")
            }
            .addOnFailureListener {
                onResult("User")
            }
    }
}