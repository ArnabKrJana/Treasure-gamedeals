package com.example.treasure.utils

import android.content.SharedPreferences
import android.util.Log
import com.example.treasure.domain.uiModels.User
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class TokenManagerTest {

    private lateinit var tokenManager: TokenManager
    private val sharedPreferences: SharedPreferences = mockk(relaxed = true)
    private val editor: SharedPreferences.Editor = mockk(relaxed = true)

    @Before
    fun setup() {
        // Mock Android Log to avoid "Method d in android.util.Log not mocked"
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        every { sharedPreferences.edit() } returns editor
        
        // Default behavior for getString
        every { sharedPreferences.getString(any(), any()) } returns null
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `isSessionActive is true if access token exists on init`() {
        every { sharedPreferences.getString("access_token", null) } returns "valid_token"
        
        tokenManager = TokenManager(sharedPreferences)
        
        assertThat(tokenManager.isSessionActive.value).isTrue()
    }

    @Test
    fun `isSessionActive is false if access token is null on init`() {
        every { sharedPreferences.getString("access_token", null) } returns null
        
        tokenManager = TokenManager(sharedPreferences)
        
        assertThat(tokenManager.isSessionActive.value).isFalse()
    }

    @Test
    fun `saveTokens stores tokens and activates session`() {
        tokenManager = TokenManager(sharedPreferences)
        
        tokenManager.saveTokens("access", "refresh")
        
        verify {
            editor.putString("access_token", "access")
            editor.putString("refresh_token", "refresh")
            editor.apply() // sharedPreferences.edit { ... } calls apply()
        }
        assertThat(tokenManager.isSessionActive.value).isTrue()
    }

    @Test
    fun `getUser returns null when user data is missing`() {
        tokenManager = TokenManager(sharedPreferences)
        every { sharedPreferences.getString("user_data", null) } returns null
        
        val result = tokenManager.getUser()
        
        assertThat(result).isNull()
    }

    @Test
    fun `getUser returns user when json is valid`() {
        tokenManager = TokenManager(sharedPreferences)
        val userJson = """{"id":1,"email":"test@example.com","fullName":"Test User","profilePicture":null,"role":"USER"}"""
        every { sharedPreferences.getString("user_data", null) } returns userJson
        
        val result = tokenManager.getUser()
        
        assertThat(result).isNotNull()
        assertThat(result?.email).isEqualTo("test@example.com")
        assertThat(result?.id).isEqualTo(1L)
    }

    @Test
    fun `getUser returns null when json is invalid (edge case)`() {
        tokenManager = TokenManager(sharedPreferences)
        every { sharedPreferences.getString("user_data", null) } returns "invalid_json"
        
        val result = tokenManager.getUser()
        
        assertThat(result).isNull()
    }

    @Test
    fun `saveUser serializes and stores user object`() {
        tokenManager = TokenManager(sharedPreferences)
        val user = User(1L, "test@example.com", "Test User", null, "USER")
        
        tokenManager.saveUser(user)
        
        verify {
            editor.putString("user_data", any())
            editor.apply()
        }
    }

    @Test
    fun `clearSession wipes data and deactivates session`() {
        tokenManager = TokenManager(sharedPreferences)
        
        tokenManager.clearSession()
        
        verify {
            editor.clear()
            editor.apply()
        }
        assertThat(tokenManager.isSessionActive.value).isFalse()
    }
}
