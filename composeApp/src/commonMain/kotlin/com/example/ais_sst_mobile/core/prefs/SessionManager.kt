package com.example.ais_sst_mobile.core.prefs

import com.russhwolf.settings.Settings

class SessionManager(private val settings: Settings) {

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
    }

    fun saveAuthToken(token: String) {
        settings.putString(KEY_TOKEN, token)
    }

    fun fetchAuthToken(): String? {
        return settings.getStringOrNull(KEY_TOKEN)
    }

    fun saveUserId(id: Int) {
        settings.putInt(KEY_USER_ID, id)
    }

    fun fetchUserId(): Int? {
        return settings.getIntOrNull(KEY_USER_ID)
    }

    fun isLoggedIn(): Boolean {
        return fetchAuthToken() != null
    }

    fun logout() {
        settings.remove(KEY_TOKEN)
        settings.remove(KEY_USER_ID)
    }
}