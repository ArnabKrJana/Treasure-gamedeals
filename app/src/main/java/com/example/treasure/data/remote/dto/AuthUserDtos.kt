package com.example.treasure.data.remote.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Network request payload submitting a Google OAuth 2.0 ID token for backend authentication.
 *
 * @property idToken Cryptographically signed JWT ID token issued by Google Identity services.
 */
@Keep
data class GoogleLoginRequest(
    @SerializedName("idToken") val idToken: String
)

/**
 * Network request payload submitting a refresh token to renew an expired JWT access token.
 *
 * @property refreshToken Long-lived refresh token string.
 */
@Keep
data class RefreshTokenRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

/**
 * Network request payload submitting an offline server authorization code to link Google Drive.
 *
 * @property serverAuthCode One-time server authorization code exchanged for Drive API refresh tokens.
 */
@Keep
data class DriveAccessRequest(
    @SerializedName("serverAuthCode") val serverAuthCode: String
)

/**
 * Authentication response payload returned upon successful login or token renewal.
 *
 * @property accessToken Short-lived JWT bearer token used to authorize subsequent REST API calls.
 * @property refreshToken Long-lived token used to obtain renewed access tokens upon expiry.
 * @property user Authenticated user identity metadata.
 */
@Keep
data class AuthResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("user") val user: UserDto
)

/**
 * Generic status message response returned by backend mutation endpoints.
 *
 * @property message Informational status message string returned by the server.
 */
@Keep
data class MessageResponse(
    @SerializedName("message") val message: String
)

/**
 * Response payload returned upon linking a Google Drive account.
 *
 * @property message Informational status message string.
 * @property driveLinked `true` if Google Drive was successfully linked and authorized.
 */
@Keep
data class DriveAccessResponse(
    @SerializedName("message") val message: String,
    @SerializedName("driveLinked") val driveLinked: Boolean
)

/**
 * Remote representation of an authenticated user identity received from the backend service.
 *
 * @property id Unique numeric user identifier in backend persistence.
 * @property email Primary registered email address.
 * @property fullName Optional display name returned from the OAuth provider.
 * @property profilePicture Optional public image URL linking to user avatar.
 * @property role Authorization security role assigned to the user ([Role]).
 */
@Keep
data class UserDto(
    @SerializedName("id") val id: Long,
    @SerializedName("email") val email: String,
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("profilePicture") val profilePicture: String?,
    @SerializedName("role") val role: Role
)

/**
 * User authorization role permissions enum matching backend Spring Security definitions.
 */
@Keep
enum class Role {
    /**
     * Standard user role.
     */
    @SerializedName("USER") USER,

    /**
     * Administrative role.
     */
    @SerializedName("ADMIN") ADMIN
}
