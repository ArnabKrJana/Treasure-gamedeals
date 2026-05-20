package com.example.treasure.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.treasure.R
import com.example.treasure.data.local.entity.DealCategory
import com.example.treasure.data.local.entity.DealEntity
import com.example.treasure.domain.uiModels.Price
import com.example.treasure.ui.theme.TreasureTheme
import com.example.treasure.ui.uiComponents.*
import com.example.treasure.ui.viewModels.DetailViewModel
import com.example.treasure.ui.uiComponents.ActionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    viewModel: DetailViewModel = hiltViewModel()
) {
    val gameDetail by viewModel.gameDetail.collectAsStateWithLifecycle()

    val wishlistItems by viewModel.repository.getWishlistItems().collectAsState(initial = emptyList())
    val isFavorite = wishlistItems.any { it.gameId == gameDetail?.id }

    DetailScreenContent(
        gameDetail = gameDetail,
        isFavorite = isFavorite,
        onToggleFavorite = { gameDetail?.let { viewModel.toggleFavorite(it) } }
    )
}

@Composable
fun DetailScreenContent(
    gameDetail: DealEntity?,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedScreenshotIndex by remember { mutableStateOf<Int?>(null) }
    var showVideoPlayer by rememberSaveable { mutableStateOf(false) }

    // --- State Maps for Tracking Action Statuses ---
    val downloadStatuses = remember { mutableStateMapOf<String, ActionStatus>() }
    val uploadStatuses = remember { mutableStateMapOf<String, ActionStatus>() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        floatingActionButton = {
            if (gameDetail != null) {
                FloatingActionButton(
                    onClick = onToggleFavorite,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from wishlist" else "Add to wishlist",
                        tint = if (isFavorite) Color(0xFFEB0076) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.surface)) {
            if (gameDetail == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // 1. Header
                    GameHeader(
                        bannerUrl = gameDetail.screenshots?.firstOrNull() ?: gameDetail.thumbnail,
                        thumbnailUrl = gameDetail.thumbnail
                    )

                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = gameDetail.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // 2. Info Rows
                        DetailInfoRow(stringResource(R.string.genre_label), gameDetail.genres?.joinToString(", ") ?: "N/A")
                        DetailInfoRow(stringResource(R.string.developer_label), gameDetail.developer ?: "N/A")
                        DetailInfoRow(stringResource(R.string.publisher_label), gameDetail.publisher ?: "N/A")
                        DetailInfoRow(stringResource(R.string.franchise_label), gameDetail.franchise ?: "N/A")
                        DetailInfoRow(stringResource(R.string.release_date_label), gameDetail.releaseDate ?: "N/A")
                        DetailInfoRow(stringResource(R.string.age_rating_label), gameDetail.maturityRating ?: "N/A")
                        DetailInfoRow(stringResource(R.string.reviews_label), gameDetail.upVotes ?: "N/A")

                        Text(
                            text = stringResource(R.string.description_label),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                        )

                        // 3. Expandable Text
                        ExpandableText(
                            text = gameDetail.description ?: "No description available."
                        )

                        // 4. Horizontal Trailer & Screenshot Gallery
                        MediaGalleryRow(
                            trailerUrl = gameDetail.trailerUrl,
                            thumbnailUrl = gameDetail.thumbnail,
                            screenshots = gameDetail.screenshots,
                            onTrailerClick = { showVideoPlayer = true },
                            onScreenshotClick = { index -> selectedScreenshotIndex = index }
                        )

                        // 5. System Requirements
                        SystemRequirementsSection(
                            currentStore = gameDetail.storeId.replaceFirstChar { it.uppercase() },
                            currentPrice = Price(gameDetail.originalPrice, gameDetail.currentPrice),
                            dealUrl = "https://store.steampowered.com/app/${gameDetail.id}",
                            otherStores = gameDetail.otherStores ?: emptyList(),
                            systemRequirements = gameDetail.systemRequirements
                        )
                    }
                }
            }

            // --- FULL SCREEN OVERLAYS --- //

            selectedScreenshotIndex?.let { index ->
                FullScreenImageOverlay(
                    screenshots = gameDetail?.screenshots ?: emptyList(),
                    initialIndex = index,
                    downloadStatuses = downloadStatuses, // Pass map
                    uploadStatuses = uploadStatuses,     // Pass map
                    onDismiss = { selectedScreenshotIndex = null },
                    onDownloadLocalClick = { url ->
                        // Simulate Download Flow (Move this to ViewModel later!)
                        downloadStatuses[url] = ActionStatus.LOADING
                        coroutineScope.launch {
                            delay(1500) // Pretend we are downloading
                            downloadStatuses[url] = ActionStatus.SUCCESS
                        }
                    },
                    onDriveSyncClick = { url ->
                        // Simulate Drive Sync Flow (Move this to ViewModel later!)
                        uploadStatuses[url] = ActionStatus.LOADING
                        coroutineScope.launch {
                            delay(1500) // Pretend we are syncing to treasure-backend
                            uploadStatuses[url] = ActionStatus.SUCCESS
                        }
                    }
                )
            }

            if (showVideoPlayer) {
                gameDetail?.trailerUrl?.let { url ->
                    VideoPlayerOverlay(
                        trailerUrl = url,
                        onDismiss = { showVideoPlayer = false }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DetailScreenPreview() {
    TreasureTheme {
        DetailScreenContent(
            gameDetail = DealEntity(
                id = "1",
                listingIndex = 0,
                title = "Senua's Saga: Hellblade II",
                thumbnail = "https://example.com/thumb.jpg",
                storeId = "steam",
                originalPrice = 59.99,
                currentPrice = 29.99,
                discountPercent = 50,
                upVotes = "Very Positive (92%)",
                upVoteColor = "green",
                category = DealCategory.HOT_DEALS,
                description = "The sequel to the award-winning Hellblade: Senua's Sacrifice...",
                genres = listOf("Action", "Adventure"),
                developer = "Ninja Theory",
                publisher = "Xbox Game Studios",
                franchise = "Ninja Theory, Hellblade Franchise",
                releaseDate = "21 May, 2024",
                maturityRating = "Mature 17+",
                screenshots = listOf("https://example.com/ss1.jpg"),
                trailerUrl = "https://example.com/trailer.mp4"
            )
        )
    }
}