package com.example.treasure.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.treasure.domain.uiModels.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    //  Create the reactive state. It defaults to true if a token already exists on cold boot.
    private val _isSessionActive = MutableStateFlow(getAccessToken() != null)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()


    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_DATA = "user_data"
    }

    // --- TOKEN MANAGEMENT ---

    fun saveTokens(accessToken: String, refreshToken: String) {
        sharedPreferences.edit {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
        }
        // 2. Broadcast that the user is logged in
        _isSessionActive.value = true
    }

    fun getAccessToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    // --- USER PROFILE MANAGEMENT ---

    fun saveUser(user: User) {
        val userJson = Json.encodeToString(user)
        sharedPreferences.edit { putString(KEY_USER_DATA, userJson) }
    }

    fun getUser(): User? {
        val userJson = sharedPreferences.getString(KEY_USER_DATA, null)
        return if (userJson != null) {
            try {
                Json.decodeFromString<User>(userJson)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    // --- SESSION CONTROL ---

    /**
     * Atomically wipes all tokens and user data.
     * Call this on Logout or when a Refresh Token expires.
     */
    fun clearSession() {
        sharedPreferences.edit { clear() }
        // Broadcast that the session is dead (Triggers the Eject Seat!)
        _isSessionActive.value = false
    }
}