package com.example.ais_sst_mobile.core.prefs

import com.example.ais_sst_mobile.domain.model.AppRole
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(private val settings: Settings) {

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_REAL_ROLE = "real_role"
        private const val KEY_ACTIVE_ROLE = "active_role"
    }

    private val _activeRoleFlow = MutableStateFlow(fetchActiveRole())
    val activeRoleFlow = _activeRoleFlow.asStateFlow()


    fun saveAuthToken(token: String) {
        settings.putString(KEY_TOKEN, token)
    }

    fun fetchAuthToken(): String? {
        return settings.getStringOrNull(KEY_TOKEN)
    }
    fun saveRefreshToken(token: String) {
        settings.putString(KEY_REFRESH_TOKEN, token)
    }

    fun fetchRefreshToken(): String? {
        return settings.getStringOrNull(KEY_REFRESH_TOKEN)
    }
    fun saveUserId(id: Int) {
        settings.putInt(KEY_USER_ID, id)
    }

    fun fetchUserId(): Int? {
        return settings.getIntOrNull(KEY_USER_ID)
    }

    fun saveRealRole(role: AppRole) {
        settings.putString(KEY_REAL_ROLE, role.serverName)

        if (settings.getStringOrNull(KEY_ACTIVE_ROLE) == null) {
            setActiveRole(role)
        }
    }

    fun getRealRole(): AppRole {
        val roleName = settings.getStringOrNull(KEY_REAL_ROLE)
        return AppRole.fromServerName(roleName)
    }

    fun setActiveRole(role: AppRole) {
        settings.putString(KEY_ACTIVE_ROLE, role.serverName)
        _activeRoleFlow.value = role
    }

    private fun fetchActiveRole(): AppRole {
        val roleName = settings.getStringOrNull(KEY_ACTIVE_ROLE)
        return AppRole.fromServerName(roleName)
    }


    fun isLoggedIn(): Boolean {
        return fetchAuthToken() != null
    }

    fun logout() {
        settings.remove(KEY_TOKEN)
        settings.remove(KEY_REFRESH_TOKEN)
        settings.remove(KEY_USER_ID)
        settings.remove(KEY_REAL_ROLE)
        settings.remove(KEY_ACTIVE_ROLE)
        _activeRoleFlow.value = AppRole.STUDENT
    }
}