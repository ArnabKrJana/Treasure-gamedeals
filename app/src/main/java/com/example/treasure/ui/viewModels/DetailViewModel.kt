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
import com.example.treasure.data.repositoryImpl.SettingsRepository
import com.example.treasure.domain.repository.GameRepository
import com.example.treasure.domain.repository.LocalDownloadRepository
import com.example.treasure.domain.uiModels.GameCardItem
import com.example.treasure.domain.uiModels.Price
import com.example.treasure.domain.uiModels.UpVotes
import com.example.treasure.ui.navigationGraphs.LeafDestination
import com.example.treasure.ui.uiComponents.ActionStatus
import com.example.treasure.utils.ColorCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * ViewModel managing the comprehensive game details view, media gallery downloads, and Google Drive cloud syncing.
 *
 * Observes Room database cache for reactive UI updates and triggers remote backend synchronization
 * to retrieve expanded deal metadata (system requirements, store links, screenshots, videos).
 *
 * @param savedStateHandle Navigation handle providing destination arguments, specifically the target game ID.
 * @property repository Game data repository providing Room observers and backend detail enrichment.
 * @property api Backend API service executing Google Drive authorization and sync requests.
 * @property localDownloadRepository Repository managing local disk downloads via Android DownloadManager.
 * @property settingsRepository Preferences repository maintaining Google Drive link status.
 */
@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val repository: GameRepository,
    private val api: TreasureBackendApi,
    private val localDownloadRepository: LocalDownloadRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val args = savedStateHandle.toRoute<LeafDestination.Detail>()

    /**
     * Unique identifier for the game deal displayed on this details screen.
     */
    val gameId = args.itemId

    /**
     * Hot observable stream emitting the cached [DealEntity] from Room, or null if loading.
     */
    val gameDetail: StateFlow<DealEntity?> = repository.observeGameDetails(gameId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * Map tracking the local disk download status for each wallpaper/media URL.
     */
    val downloadStatuses: StateFlow<Map<String, ActionStatus>> = localDownloadRepository.getDownloadedAssetIds()
        .map { downloadedUrls ->
            downloadedUrls.associateWith { ActionStatus.SUCCESS }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /**
     * Observable stream indicating whether the user has linked a Google Drive account for cloud wallpaper syncing.
     */
    val isDriveLinked: StateFlow<Boolean> = settingsRepository.isDriveLinked
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _uploadStatuses = MutableStateFlow<Map<String, ActionStatus>>(emptyMap())

    /**
     * Map tracking the active cloud upload/sync status for media asset URLs.
     */
    val uploadStatuses: StateFlow<Map<String, ActionStatus>> = _uploadStatuses.asStateFlow()

    private val _driveAuthEvent = MutableSharedFlow<Unit>()

    /**
     * One-shot event stream signalling that Google Drive OAuth re-authorization is required from the UI.
     */
    val driveAuthEvent = _driveAuthEvent.asSharedFlow()

    private val pollingJobs = mutableMapOf<String, Job>()

    init {
        fetchGameDetails()
    }

    /**
     * Triggers remote backend enrichment to populate full screenshots, videos, and system specifications.
     */
    private fun fetchGameDetails() {
        viewModelScope.launch {
            repository.fetchAndEnrichGameDetails(gameId)
        }
    }

    /**
     * Toggles the wishlist status of the current game deal in Room and synchronizes with cloud if logged in.
     *
     * @param deal The [DealEntity] being added or removed from favorites.
     */
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
        store = storeId,
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

    /**
     * Initiates a local filesystem wallpaper download via Android's DownloadManager.
     *
     * @param imageUrl Remote URL of the wallpaper/screenshot asset.
     */
    fun downloadWallpaperLocal(imageUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val fileName = "Treasure_${gameId}_${System.currentTimeMillis()}.jpg"
            localDownloadRepository.downloadWallpaper(imageUrl, imageUrl, fileName)
        }
    }

    /**
     * Initiates Google Drive synchronization for an image asset. If Drive is unlinked,
     * emits an auth request event via [driveAuthEvent].
     *
     * @param imageUrl Remote URL of the media asset to sync.
     */
    fun initiateDriveSync(imageUrl: String) {
        if (!isDriveLinked.value) {
            viewModelScope.launch { _driveAuthEvent.emit(Unit) }
            return
        }
        syncWallpaperToDrive(imageUrl)
    }

    /**
     * Exchanges a Google OAuth server authorization code for backend Google Drive tokens.
     *
     * @param serverAuthCode OAuth authorization code from Google Sign-In.
     * @param pendingImageUrl Optional image URL that triggered the authorization flow, queued immediately upon success.
     */
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
                // Ignore: User can retry the sync action
            }
        }
    }

    /**
     * Queues an asynchronous wallpaper sync request with the backend and begins polling for status completion.
     *
     * @param imageUrl Remote URL of the media asset.
     */
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

    /**
     * Polls the backend endpoint periodically until the queued cloud sync job completes or fails.
     *
     * @param imageUrl URL of the media asset whose sync job is being polled.
     */
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
                    // Retry on transient network blip
                }

                delay(3.seconds)
            }
        }
    }

    /**
     * Cancels all active background polling jobs when the ViewModel is destroyed.
     */
    override fun onCleared() {
        super.onCleared()
        pollingJobs.values.forEach { it.cancel() }
    }
}
