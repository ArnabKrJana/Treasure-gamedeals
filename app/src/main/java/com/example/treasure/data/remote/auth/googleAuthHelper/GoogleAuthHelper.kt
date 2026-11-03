package com.example.treasure.data.remote.auth.googleAuthHelper

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.example.treasure.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import javax.inject.Inject

/**
 * Architectural helper contract defining Google Identity authorization workflows.
 *
 * Utilizes Android's modern [CredentialManager] API to prompt users for Google Account selection
 * and retrieve authenticated Google ID tokens for backend exchange.
 */
interface GoogleAuthHelper {
    /**
     * Initiates the Google Sign-In bottom sheet interaction and retrieves an OAuth 2.0 ID token.
     *
     * @param context Host Android context used to instantiate [CredentialManager].
     * @return Google ID token string if authorization succeeded, or `null` if cancelled or failed.
     */
    suspend fun signIn(context: Context): String?
}

/**
 * Concrete implementation of [GoogleAuthHelper] utilizing AndroidX [CredentialManager] and Google ID Library.
 *
 * Configures [GetGoogleIdOption] with the registered Web Client ID, enables auto-selection for returning users,
 * and extracts the raw JWT ID token string from the received [GoogleIdTokenCredential].
 */
class GoogleAuthHelperImpl @Inject constructor() : GoogleAuthHelper {

    /**
     * Executes the Google Credential Manager request.
     *
     * @param context Host Android context.
     * @return Extracted Google OAuth ID token string, or `null` on error or cancellation.
     */
    override suspend fun signIn(context: Context): String? {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
