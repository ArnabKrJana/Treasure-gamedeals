package com.example.treasure.data.remote.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * Generic container model matching Spring Data `org.springframework.data.domain.Page<T>` JSON serialization.
 *
 * Encapsulates the current page slice of records alongside pagination metadata, consumed by
 * [com.example.treasure.data.local.remoteMediators.DealRemoteMediator] to navigate cursor boundaries.
 *
 * @param T The payload item DTO type contained within the page slice.
 * @property content List of payload items contained within this page slice.
 * @property pageable Unstructured page request metadata.
 * @property totalElements Total count of records matching the query across all pages.
 * @property totalPages Total number of pages available in the paginated dataset.
 * @property last `true` if this is the final page in the dataset, signaling pagination completion.
 * @property size Configured maximum element count per page slice.
 * @property number Zero-indexed page number of the current slice.
 * @property sort Sorting descriptor metadata.
 * @property numberOfElements Count of elements present in this specific page slice.
 * @property first `true` if this is the initial page in the dataset.
 * @property empty `true` if the page contains zero elements.
 */
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
