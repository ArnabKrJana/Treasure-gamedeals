package com.example.treasure.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.treasure.data.local.entity.UserInteractionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserInteractionDao {


    @Query("SELECT * FROM user_interactions WHERE isAddedToCart = 1 ORDER BY timestamp DESC")
    fun getCartItems(): Flow<@JvmSuppressWildcards List<UserInteractionEntity>>

    @Query("SELECT * FROM user_interactions WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavorites(): Flow<@JvmSuppressWildcards List<UserInteractionEntity>>

    @Query("SELECT gameId FROM user_interactions WHERE isFavorite = 1")
    fun getAllInteractedIds(): Flow<@JvmSuppressWildcards List<String>>

    @Query("SELECT * FROM user_interactions WHERE gameId = :gameId")
    fun getInteractionForGame(gameId: String): UserInteractionEntity?

    // Returns row ID (Long) synchronously.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertInteraction(interaction: UserInteractionEntity): Long
}