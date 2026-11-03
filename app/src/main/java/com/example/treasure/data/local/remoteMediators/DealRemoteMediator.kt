package com.example.treasure.data.local.remoteMediators

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.treasure.data.local.TreasureDatabase
import com.example.treasure.data.local.entity.DealCategory
import com.example.treasure.data.local.entity.DealEntity
import com.example.treasure.data.local.entity.RemoteKeys
import com.example.treasure.data.remote.apiService.TreasureBackendApi
import com.example.treasure.data.toEntity
import retrofit2.HttpException
import java.io.IOException

/**
 * Paging 3 [RemoteMediator] orchestrating the offline-first network-to-database synchronization pipeline.
 *
 * Intercepts pagination boundary signals from the UI's [androidx.paging.Pager], fetches pages from the
 * Spring Boot backend API, and writes them atomically into local Room persistence via [withTransaction].
 * Room acts as the Single Source of Truth (SSOT), which automatically notifies the UI via [androidx.paging.PagingSource].
 *
 * Synchronization Mechanics:
 * - [LoadType.REFRESH]: Fetches page 0. In an atomic transaction, invalidates previous cache entries for the
 *   given category via [com.example.treasure.data.local.dao.DealDao.clearDealsByCategory] and resets cursor keys.
 * - [LoadType.PREPEND]: Checks [getRemoteKeyForFirstItem] for a valid previous page key. If none exists, ends prepending.
 * - [LoadType.APPEND]: Checks [getRemoteKeyForLastItem] for the subsequent page key and fetches next batches.
 *
 * @param db Central [TreasureDatabase] providing transactional execution and DAO access.
 * @param service Backend REST client ([TreasureBackendApi]) providing paginated deal responses.
 * @param category Target [DealCategory] defining which partition of deals to synchronize.
 */
@OptIn(ExperimentalPagingApi::class)
class DealRemoteMediator(
    private val db: TreasureDatabase,
    private val service: TreasureBackendApi,
    private val category: DealCategory
) : RemoteMediator<Int, DealEntity>() {

    private val remoteKeysDao = db.remoteKeysDao()
    private val dealDao = db.dealDao()

    /**
     * Executes the paging fetch, pagination cursor calculation, and database persistence transaction.
     *
     * @param loadType Indicates whether the fetch is a [LoadType.REFRESH], [LoadType.PREPEND], or [LoadType.APPEND].
     * @param state Current snapshot state of loaded pages, anchor positions, and [androidx.paging.PagingConfig].
     * @return [MediatorResult.Success] indicating whether the end of pagination was reached, or [MediatorResult.Error].
     */
    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, DealEntity>
    ): MediatorResult {
        return try {
            val pageToBeFetched = when (loadType) {
                LoadType.REFRESH -> {
                    0
                }

                LoadType.PREPEND -> {
                    val remoteKey = getRemoteKeyForFirstItem(state)
                    val previousPage = remoteKey?.prevKey ?: return MediatorResult.Success(
                        endOfPaginationReached = remoteKey != null
                    )
                    previousPage
                }

                LoadType.APPEND -> {
                    val remoteKey = getRemoteKeyForLastItem(state)
                    val nextPage = remoteKey?.nextKey ?: return MediatorResult.Success(
                        endOfPaginationReached = remoteKey != null
                    )
                    nextPage
                }
            }

            val size = state.config.pageSize

            val response = when (category) {
                DealCategory.HOT_DEALS,
                DealCategory.LOWEST_PRICE -> {
                    service.getDeals(
                        category = category.name,
                        page = pageToBeFetched,
                        size = size
                    )
                }

                DealCategory.MAC_DEALS -> {
                    service.getPlatformDeals(
                        platformId = 2,
                        page = pageToBeFetched,
                        size = size
                    )
                }

                DealCategory.LINUX_DEALS -> {
                    service.getPlatformDeals(
                        platformId = 3,
                        page = pageToBeFetched,
                        size = size
                    )
                }

                DealCategory.SEARCH -> {
                    throw IllegalArgumentException(
                        "SEARCH category should not use RemoteMediator"
                    )
                }
            }

            if (!response.isSuccessful) {
                MediatorResult.Error(HttpException(response))
            } else {
                val pageData = response.body()
                val data = pageData?.content ?: emptyList()
                val endOfPaginationReached = pageData?.last ?: true

                db.withTransaction {
                    if (loadType == LoadType.REFRESH) {
                        remoteKeysDao.clearRemoteKeysByPattern("%_${category.name}")
                        dealDao.clearDealsByCategory(category = category)
                    }

                    val prevKey = if (pageToBeFetched == 0) null else pageToBeFetched - 1
                    val nextKey = if (endOfPaginationReached) null else pageToBeFetched + 1

                    val remoteKeys = data.map { dealDto ->
                        RemoteKeys(
                            queryId = "${dealDto.id}_${category.name}",
                            prevKey = prevKey,
                            nextKey = nextKey
                        )
                    }

                    val offset = pageToBeFetched * size
                    val entities = data.mapIndexed { index, dealDto ->
                        dealDto.toEntity(category, listingIndex = offset + index)
                    }

                    remoteKeysDao.insertAll(remoteKeys)
                    dealDao.insertDeals(entities)
                }
                MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
            }
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }

    /**
     * Resolves the [RemoteKeys] instance closest to the user's current scroll viewport anchor position.
     *
     * @param state Snapshot state of currently loaded pages.
     * @return Matching [RemoteKeys] cursor or `null`.
     */
    private suspend fun getRemoteKeyToTheClosestPosition(state: PagingState<Int, DealEntity>): RemoteKeys? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.let { deal ->
                remoteKeysDao.remoteKeysId("${deal.id}_${category.name}")
            }
        }
    }

    /**
     * Resolves the [RemoteKeys] instance associated with the first item in the loaded dataset for prepending.
     *
     * @param state Snapshot state of currently loaded pages.
     * @return Matching [RemoteKeys] cursor or `null`.
     */
    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, DealEntity>): RemoteKeys? {
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()?.let { deal ->
            remoteKeysDao.remoteKeysId("${deal.id}_${category.name}")
        }
    }

    /**
     * Resolves the [RemoteKeys] instance associated with the last item in the loaded dataset for appending.
     *
     * @param state Snapshot state of currently loaded pages.
     * @return Matching [RemoteKeys] cursor or `null`.
     */
    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, DealEntity>): RemoteKeys? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()?.let { deal ->
            remoteKeysDao.remoteKeysId("${deal.id}_${category.name}")
        }
    }
}
