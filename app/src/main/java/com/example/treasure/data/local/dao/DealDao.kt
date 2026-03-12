package com.example.treasure.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.paging.PagingSource
import com.example.treasure.data.local.entity.DealCategory
import com.example.treasure.data.local.entity.DealEntity
import com.example.treasure.data.local.entity.SystemRequirementEntity
import com.example.treasure.domain.uiModels.StoreDeal
import kotlinx.coroutines.flow.Flow

@Dao
interface DealDao {

    //Home Screen
    @Query("SELECT * FROM deals WHERE category = :category ORDER BY listingIndex ASC")
    fun getDealsByCategory(category: DealCategory): PagingSource<Int, DealEntity>


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDeals(deals: List<DealEntity>): @JvmSuppressWildcards List<Long>

    // Clears cache for refresh
    @Query("DELETE FROM deals WHERE category = :category")
    suspend fun clearDealsByCategory(category: DealCategory) : @JvmSuppressWildcards Int


    //Detail screen
    @Query("SELECT * FROM deals WHERE id = :dealId LIMIT 1")
    fun observeDealById(dealId: String): Flow<DealEntity?>

    // --- ENRICHMENT (The Lazy Load Update) ---
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
    ) : @JvmSuppressWildcards Int
}