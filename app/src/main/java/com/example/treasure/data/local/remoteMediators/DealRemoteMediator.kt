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
import com.example.treasure.data.remote.apiService.ItadApi
import com.example.treasure.data.toEntity

import retrofit2.HttpException
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class DealRemoteMediator(
    private val db: TreasureDatabase,
    private val service: ItadApi,
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
                    remoteKey?.nextKey?.minus(1) ?: 1
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
            //calculating offset
            val limit = state.config.pageSize
            val offset = (pageToBeFetched - 1) * limit
            val response = service.getDeals(
                country = "IN",
                sort = if (category == DealCategory.HOT_DEALS) "-hot" else "-cut",
                shops = "61,35,16",
                limit = limit,
                offset = offset
            )
            if (!response.isSuccessful) {
                return MediatorResult.Error(HttpException(response))
            } else {
                val data = response.body()?.list ?: emptyList()
                val endOfPaginationReached = data.isEmpty()
                //Save to Database
                db.withTransaction {
                    if (loadType == LoadType.REFRESH) {
                        remoteKeysDao.clearRemoteKeysByPattern("%_${category.name}")
                        dealDao.clearDealsByCategory(category = category)
                    }
                    val prevKey = if (pageToBeFetched == 1) null else pageToBeFetched - 1
                    val nextKey = if (endOfPaginationReached) null else pageToBeFetched + 1

                    val remoteKeys = data.map { dealDto ->
                        RemoteKeys(
                            queryId = "${dealDto.id}_${category.name}",
                            prevKey = prevKey,
                            nextKey = nextKey
                        )
                    }
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


    private suspend fun getRemoteKeyToTheClosestPosition(state: PagingState<Int, DealEntity>)
            : RemoteKeys? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.let { deal ->
                remoteKeysDao.remoteKeysId("${deal.id}_${category.name}")

            }
        }
    }

    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, DealEntity>)
            : RemoteKeys? {
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()?.let { deal ->
            remoteKeysDao.remoteKeysId("${deal.id}_${category.name}")

        }
    }

    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, DealEntity>)
            : RemoteKeys? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()?.let { deal ->
            remoteKeysDao.remoteKeysId("${deal.id}_${category.name}")

        }
    }
}