package com.example.treasure.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.treasure.data.local.entity.RemoteKeys

/**
 * Data Access Object (DAO) managing Paging 3 pagination cursor keys.
 *
 * Utilized by [com.example.treasure.data.local.remoteMediators.DealRemoteMediator] to track
 * previous and next page indices across network-to-database pagination cycles.
 */
@Dao
interface RemoteKeysDao {

    /**
     * Inserts or replaces a collection of remote pagination keys.
     *
     * @param remoteKey List of [RemoteKeys] cursor entries to persist.
     * @return Generated row identifiers for the inserted keys.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(remoteKey: List<RemoteKeys>): @JvmSuppressWildcards List<Long>

    /**
     * Retrieves the pagination key record corresponding to a composite query identifier (`${dealId}_${category}`).
     *
     * @param queryId Composite query identifier string.
     * @return The matching [RemoteKeys] instance, or `null` if not found.
     */
    @Query("SELECT * FROM remote_keys WHERE queryId = :queryId")
    suspend fun remoteKeysId(queryId: String): @JvmSuppressWildcards RemoteKeys?

    /**
     * Deletes all remote keys matching a SQL `LIKE` pattern (e.g., `%_HOT_DEALS`) upon cache invalidation.
     *
     * @param pattern SQL `LIKE` wildcard pattern matching the target category suffix.
     * @return The number of affected database rows.
     */
    @Query("DELETE FROM remote_keys WHERE queryId LIKE :pattern")
    suspend fun clearRemoteKeysByPattern(pattern: String): @JvmSuppressWildcards Int
}
