package com.example.financetracker

import android.content.Context
import android.content.SharedPreferences

class AuthManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME      = "auth_prefs"
        private const val KEY_IS_LOGGED  = "is_logged_in"
        private const val VALID_USERNAME = "admin"
        private const val VALID_PASSWORD = "admin"
    }

    fun checkCredentials(username: String, password: String): Boolean =
        username == VALID_USERNAME && password == VALID_PASSWORD

    fun saveLoginState(isLoggedIn: Boolean) {
        prefs.edit().putBoolean(KEY_IS_LOGGED, isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED, false)

    fun logout() {
        prefs.edit().putBoolean(KEY_IS_LOGGED, false).apply()
    }
}