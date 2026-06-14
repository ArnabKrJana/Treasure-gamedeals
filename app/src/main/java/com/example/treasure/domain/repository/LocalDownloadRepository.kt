package com.example.treasure.domain.repository


import kotlinx.coroutines.flow.Flow

interface LocalDownloadRepository {
    suspend fun downloadWallpaper(assetId: String, url: String, fileName: String): Long
    fun getDownloadedAssetIds(): Flow<Set<String>>
}