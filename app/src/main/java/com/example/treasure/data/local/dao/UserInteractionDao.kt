package com.example.treasure.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.treasure.data.local.entity.UserInteractionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing local user interactions (cart items and favorited/wishlisted games).
 *
 * Provides reactive [Flow] streams for UI screens and synchronous one-shot queries for transactional mutations.
 */
@Dao
interface UserInteractionDao {

    /**
     * Observes all items currently added to the user's shopping cart, ordered by modification time.
     *
     * @return Cold [Flow] emitting the list of cart [UserInteractionEntity] records.
     */
    @Query("SELECT * FROM user_interactions WHERE isAddedToCart = 1 ORDER BY timestamp DESC")
    fun getCartItems(): Flow<@JvmSuppressWildcards List<UserInteractionEntity>>

    /**
     * Observes all games marked as favorites or wishlisted, ordered by timestamp in descending order.
     *
     * @return Cold [Flow] emitting the list of favorited [UserInteractionEntity] records.
     */
    @Query("SELECT * FROM user_interactions WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavorites(): Flow<@JvmSuppressWildcards List<UserInteractionEntity>>

    /**
     * Observes the list of all game IDs currently marked as favorites.
     *
     * Consumed by catalog listing components to perform O(1) membership checks for favorited visual badges.
     *
     * @return Cold [Flow] emitting the list of favorited game ID strings.
     */
    @Query("SELECT gameId FROM user_interactions WHERE isFavorite = 1")
    fun getAllInteractedIds(): Flow<@JvmSuppressWildcards List<String>>

    /**
     * Synchronously queries the interaction state for a specific game title.
     *
     * @param gameId Unique identifier of the game.
     * @return The existing [UserInteractionEntity], or `null` if no prior interaction exists.
     */
    @Query("SELECT * FROM user_interactions WHERE gameId = :gameId")
    fun getInteractionForGame(gameId: String): UserInteractionEntity?

    /**
     * Persists or replaces a user interaction record.
     *
     * @param interaction The [UserInteractionEntity] to insert or update.
     * @return Generated row identifier.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertInteraction(interaction: UserInteractionEntity): Long

    /**
     * Clears all favorite flags across interaction records upon logout or state reset.
     *
     * @return The number of affected database rows.
     */
    @Query("UPDATE user_interactions SET isFavorite = 0")
    fun clearAllFavorites(): @JvmSuppressWildcards Int
}
