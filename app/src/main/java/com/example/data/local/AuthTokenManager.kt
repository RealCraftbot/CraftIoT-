package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class AuthTokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("craft_secure_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_JWT_TOKEN = "jwt_token"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_ORG = "user_org"
    }

    fun saveSession(token: String, email: String, name: String, role: String, org: String) {
        prefs.edit().apply {
            putString(KEY_JWT_TOKEN, token)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_ROLE, role)
            putString(KEY_USER_ORG, org)
            apply()
        }
    }

    fun getJwtToken(): String? {
        return prefs.getString(KEY_JWT_TOKEN, null)
    }

    fun getUserEmail(): String? {
        return prefs.getString(KEY_USER_EMAIL, null)
    }

    fun getUserName(): String? {
        return prefs.getString(KEY_USER_NAME, null)
    }

    fun getUserRole(): String? {
        return prefs.getString(KEY_USER_ROLE, "Enterprise Administrator")
    }

    fun getUserOrg(): String? {
        return prefs.getString(KEY_USER_ORG, "Craft Innovations")
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun hasActiveSession(): Boolean {
        return getJwtToken() != null
    }
}
