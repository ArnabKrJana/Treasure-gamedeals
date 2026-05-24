package com.example.treasure.data.remote.dto

data class SpringPageResponse<T>(
    val content: List<T>,
    val pageable: Any?, // Can be ignored/Any? if you don't need the internal pageable sorting details
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean,
    val size: Int,
    val number: Int,
    val sort: Any?,
    val numberOfElements: Int,
    val first: Boolean,
    val empty: Boolean
)