package com.example.treasure.ui.viewModels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.treasure.data.local.entity.DealEntity
import com.example.treasure.data.remote.apiService.TreasureBackendApi
import com.example.treasure.data.remote.dto.DriveAccessRequest
import com.example.treasure.data.remote.dto.DriveSyncItemDto
import com.example.treasure.data.remote.dto.DriveSyncRequest
import com.example.treasure.domain.repository.GameRepository
import com.example.treasure.domain.repository.LocalDownloadRepository
import com.example.treasure.data.repositoryImpl.SettingsRepository
import com.example.treasure.domain.uiModels.GameCardItem
import com.example.treasure.domain.uiModels.Price
import com.example.treasure.domain.uiModels.UpVotes
import com.example.treasure.ui.navigationGraphs.LeafDestination
import com.example.treasure.ui.uiComponents.ActionStatus
import com.example.treasure.utils.ColorCode // Make sure this is imported!
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val repository: GameRepository,
    private val api: TreasureBackendApi,
    private val localDownloadRepository: LocalDownloadRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val args = savedStateHandle.toRoute<LeafDestination.Detail>()
    val gameId = args.itemId

    val gameDetail: StateFlow<DealEntity?> = repository.observeGameDetails(gameId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val downloadStatuses: StateFlow<Map<String, ActionStatus>> = localDownloadRepository.getDownloadedAssetIds()
        .map { downloadedUrls ->
            downloadedUrls.associateWith { ActionStatus.SUCCESS }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val isDriveLinked: StateFlow<Boolean> = settingsRepository.isDriveLinked
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _uploadStatuses = MutableStateFlow<Map<String, ActionStatus>>(emptyMap())
    val uploadStatuses = _uploadStatuses.asStateFlow()

    private val _driveAuthEvent = MutableSharedFlow<Unit>()
    val driveAuthEvent = _driveAuthEvent.asSharedFlow()

    private val pollingJobs = mutableMapOf<String, Job>()

    init {
        fetchGameDetails()
    }

    private fun fetchGameDetails() {
        viewModelScope.launch {
            repository.fetchAndEnrichGameDetails(gameId)
        }
    }

    fun toggleFavorite(deal: DealEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(deal.toGameCardItem())
        }
    }

    private fun DealEntity.toGameCardItem() = GameCardItem(
        id = id,
        listingIndex = 0,
        title = title,
        thumbnail = thumbnail,
        store = storeId, // storeId is already non-nullable, no ?: needed
        price = Price(originalPrice, currentPrice),
        upVotes = if (upVotes != null) UpVotes(upVotes, safeColorCode(upVoteColor)) else null,
        genres = genres ?: emptyList()
    )

    private fun safeColorCode(colorString: String?): ColorCode {
        return try {
            ColorCode.valueOf(colorString?.uppercase() ?: "UNKNOWN")
        } catch (_: Exception) {
            ColorCode.RED
        }
    }

    fun downloadWallpaperLocal(imageUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val fileName = "Treasure_${gameId}_${System.currentTimeMillis()}.jpg"
            localDownloadRepository.downloadWallpaper(imageUrl, imageUrl, fileName)
        }
    }

    fun initiateDriveSync(imageUrl: String) {
        if (!isDriveLinked.value) {
            viewModelScope.launch { _driveAuthEvent.emit(Unit) }
            return
        }
        syncWallpaperToDrive(imageUrl)
    }


    fun linkDriveAccount(serverAuthCode: String, pendingImageUrl: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = DriveAccessRequest(serverAuthCode)
                val response = api.linkGoogleDrive(request)

                if (response.isSuccessful && response.body()?.driveLinked == true) {
                    settingsRepository.setDriveLinked(true)

                    if (pendingImageUrl != null) {
                        syncWallpaperToDrive(pendingImageUrl)
                    }
                }
            } catch (_: Exception) {
                // Ignore: User can just click the sync button again
            }
        }
    }

    private fun syncWallpaperToDrive(imageUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uploadStatuses.update { it + (imageUrl to ActionStatus.LOADING) }

            try {
                val request = DriveSyncRequest(listOf(DriveSyncItemDto(gameId, imageUrl)))
                val response = api.queueDriveSync(request)

                if (response.isSuccessful) {
                    startPollingStatus(imageUrl)
                } else {
                    _uploadStatuses.update { it - imageUrl }
                }
            } catch (_: Exception) {
                _uploadStatuses.update { it - imageUrl }
            }
        }
    }

    private fun startPollingStatus(imageUrl: String) {
        pollingJobs[imageUrl]?.cancel()

        pollingJobs[imageUrl] = viewModelScope.launch(Dispatchers.IO) {

            while (currentCoroutineContext().isActive) {
                try {
                    val response = api.checkDriveSyncStatus(listOf(gameId))
                    val statusObj = response.body()?.firstOrNull { it.gameId == gameId }

                    if (statusObj != null) {
                        when (statusObj.status) {
                            "SUCCESS" -> {
                                _uploadStatuses.update { it + (imageUrl to ActionStatus.SUCCESS) }
                                break
                            }
                            "FAILED" -> {
                                if (statusObj.errorMessage == "AUTH_REVOKED") {
                                    settingsRepository.setDriveLinked(false)
                                    _driveAuthEvent.emit(Unit)
                                }
                                _uploadStatuses.update { it - imageUrl }
                                break
                            }
                            "PENDING" -> { /* Keep polling */ }
                        }
                    }
                } catch (_: Exception) {
                    // Network blip, ignore and keep trying
                }

                delay(3.seconds)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJobs.values.forEach { it.cancel() }
    }
}