package com.example.treasure.data.remote.dto


// --- REQUESTS ---

data class GoogleLoginRequest(
    val idToken: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class DriveAccessRequest(
    val serverAuthCode: String
)

// --- RESPONSES ---

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDto
)

data class MessageResponse(
    val message: String
)

data class DriveAccessResponse(
    val message: String,
    val driveLinked: Boolean
)

data class UserDto(
    val id: Long,
    val email: String,
    val fullName: String?,
    val profilePicture: String?,
    val role: Role
)

// You will need this enum on the Android side to match the backend
enum class Role {
    USER, ADMIN
}