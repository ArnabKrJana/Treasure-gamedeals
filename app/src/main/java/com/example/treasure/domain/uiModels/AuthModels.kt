package com.example.treasure.domain.uiModels

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long,
    val email: String,
    val fullName: String?,
    val profilePicture: String?,
    val role: String
)