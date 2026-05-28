package com.example.treasure.data.remote.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class SpringPageResponse<T>(
    @SerializedName("content") val content: List<T>,
    @SerializedName("pageable") val pageable: Any?,
    @SerializedName("totalElements") val totalElements: Long,
    @SerializedName("totalPages") val totalPages: Int,
    @SerializedName("last") val last: Boolean,
    @SerializedName("size") val size: Int,
    @SerializedName("number") val number: Int,
    @SerializedName("sort") val sort: Any?,
    @SerializedName("numberOfElements") val numberOfElements: Int,
    @SerializedName("first") val first: Boolean,
    @SerializedName("empty") val empty: Boolean
)