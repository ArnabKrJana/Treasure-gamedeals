package com.example.treasure.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.treasure.data.local.entity.RemoteKeys

@Dao
interface RemoteKeysDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(remoteKey: List<RemoteKeys>):@JvmSuppressWildcards List<Long>

    @Query("SELECT * FROM remote_keys WHERE queryId = :queryId")
    suspend fun remoteKeysId(queryId: String): @JvmSuppressWildcards RemoteKeys?

    @Query("DELETE FROM remote_keys WHERE queryId LIKE :pattern")
    suspend fun clearRemoteKeysByPattern(pattern: String) : @JvmSuppressWildcards Int
}