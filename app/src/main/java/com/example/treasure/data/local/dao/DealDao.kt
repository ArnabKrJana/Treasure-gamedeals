package com.example.treasure.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.treasure.data.local.entity.DealCategory
import com.example.treasure.data.local.entity.DealEntity
import com.example.treasure.data.local.entity.SystemRequirementEntity
import com.example.treasure.domain.uiModels.StoreDeal
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for querying, inserting, and lazily enriching cached game deals.
 *
 * Backs Paging 3 paginated feeds via [PagingSource] and provides reactive [Flow] streams
 * for game detail views.
 */
@Dao
interface DealDao {

    /**
     * Generates a [PagingSource] streaming cached deals filtered by [DealCategory], ordered by listing index.
     *
     * @param category The [DealCategory] partition (e.g., HOT_DEALS, LOWEST_PRICE, MAC_DEALS).
     * @return [PagingSource] utilized by Paging 3 [androidx.paging.Pager] to produce reactive pages.
     */
    @Query("SELECT * FROM deals WHERE category = :category ORDER BY listingIndex ASC")
    fun getDealsByCategory(category: DealCategory): PagingSource<Int, DealEntity>

    /**
     * Inserts a batch of game deals into the local database, ignoring existing rows to prevent overwriting.
     *
     * @param deals List of [DealEntity] instances to insert.
     * @return List of row IDs generated for newly inserted rows, or -1 for ignored conflicts.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDeals(deals: List<DealEntity>): @JvmSuppressWildcards List<Long>

    /**
     * Deletes all cached deals belonging to the specified category during refresh invalidation.
     *
     * @param category The [DealCategory] partition to wipe.
     * @return The number of rows deleted.
     */
    @Query("DELETE FROM deals WHERE category = :category")
    suspend fun clearDealsByCategory(category: DealCategory): @JvmSuppressWildcards Int

    /**
     * Observes real-time changes to an individual deal entity identified by its unique deal ID.
     *
     * @param dealId Unique identifier of the deal.
     * @return Cold [Flow] emitting the cached [DealEntity], or `null` if not found.
     */
    @Query("SELECT * FROM deals WHERE id = :dealId LIMIT 1")
    fun observeDealById(dealId: String): Flow<DealEntity?>

    /**
     * Lazily updates a deal record with detailed metadata fetched from external storefront APIs.
     *
     * Enriches screenshots, description, alternative store offers, developers, publishers,
     * franchise, release dates, maturity ratings, system requirements, and trailer streaming URLs.
     *
     * @param dealId Unique identifier of the target deal entity.
     * @param description Narrative plot overview or synopsis.
     * @param screenshots List of high-resolution screenshot image URLs.
     * @param otherDeals Alternative digital storefront pricing offers.
     * @param developer Development studio name.
     * @param publisher Publishing organization name.
     * @param franchise Game intellectual property franchise series.
     * @param releaseDate Human-readable release date string.
     * @param maturity Age rating classification code.
     * @param sysReqs Minimum and recommended PC hardware requirements.
     * @param trailerUrl Video trailer streaming URL.
     * @return The number of affected database rows.
     */
    @Query("""
        UPDATE deals SET 
        description = :description, 
        screenshots = :screenshots,
        otherStores = :otherDeals,
        developer = :developer,
        publisher = :publisher,
        franchise = :franchise,
        releaseDate = :releaseDate,
        maturityRating = :maturity,
        systemRequirements = :sysReqs,
        trailerUrl = :trailerUrl
        WHERE id = :dealId
    """)
    suspend fun updateDealDetails(
        dealId: String,
        description: String?,
        screenshots: List<String>?,
        otherDeals: List<StoreDeal>?,
        developer: String?,
        publisher: String?,
        franchise: String?,
        releaseDate: String?,
        maturity: String?,
        sysReqs: @JvmSuppressWildcards List<SystemRequirementEntity>?,
        trailerUrl: String?
    ): @JvmSuppressWildcards Int
}
