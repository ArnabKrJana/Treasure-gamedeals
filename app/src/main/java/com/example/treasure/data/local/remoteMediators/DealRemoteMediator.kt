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

@OptIn(ExperimentalPagingApi::class)
class DealRemoteMediator(
    private val db: TreasureDatabase,
    private val service: TreasureBackendApi,
    private val category: DealCategory
) : RemoteMediator<Int, DealEntity>() {

    private val remoteKeysDao = db.remoteKeysDao()
    private val dealDao = db.dealDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, DealEntity>
    ): MediatorResult {
        return try {
            val pageToBeFetched = when (loadType) {
                LoadType.REFRESH -> {
                    val remoteKey = getRemoteKeyToTheClosestPosition(state)
                    // Spring Boot pages are 0-indexed. If no key, start at 0.
                    remoteKey?.nextKey?.minus(1) ?: 0
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

            // Map your Android enum to the exact string your Spring Boot @RequestParam expects
            // Looking at your GameDealDtos, DealCategory has a categoryName property
            val categoryString = category.name

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
                return MediatorResult.Error(HttpException(response))
            } else {
                val pageData = response.body()
                val data = pageData?.content ?: emptyList()

                // Spring Boot's Page automatically tells us if it's the last page!
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

    private suspend fun getRemoteKeyToTheClosestPosition(state: PagingState<Int, DealEntity>): RemoteKeys? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.let { deal ->
                remoteKeysDao.remoteKeysId("${deal.id}_${category.name}")
            }
        }
    }

    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, DealEntity>): RemoteKeys? {
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()?.let { deal ->
            remoteKeysDao.remoteKeysId("${deal.id}_${category.name}")
        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, DealEntity>): RemoteKeys? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()?.let { deal ->
            remoteKeysDao.remoteKeysId("${deal.id}_${category.name}")
        }
    }
}