package com.example.treasure.data.remote.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

// --- REQUESTS ---

@Keep
data class GoogleLoginRequest(
    @SerializedName("idToken") val idToken: String
)

@Keep
data class RefreshTokenRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

@Keep
data class DriveAccessRequest(
    @SerializedName("serverAuthCode") val serverAuthCode: String
)

// --- RESPONSES ---

@Keep
data class AuthResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("user") val user: UserDto // Mapped to UserDto!
)

@Keep
data class MessageResponse(
    @SerializedName("message") val message: String
)

@Keep
data class DriveAccessResponse(
    @SerializedName("message") val message: String,
    @SerializedName("driveLinked") val driveLinked: Boolean
)

@Keep
data class UserDto(
    @SerializedName("id") val id: Long,
    @SerializedName("email") val email: String,
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("profilePicture") val profilePicture: String?,
    @SerializedName("role") val role: Role
)

// The Enum must also be protected so Gson can map "USER" or "ADMIN" strings to the object
@Keep
enum class Role {
    @SerializedName("USER") USER,
    @SerializedName("ADMIN") ADMIN
}