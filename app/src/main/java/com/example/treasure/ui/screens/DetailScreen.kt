package com.example.treasure.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.example.treasure.R
import com.example.treasure.data.local.entity.DealCategory
import com.example.treasure.data.local.entity.DealEntity
import com.example.treasure.domain.uiModels.Price
import com.example.treasure.ui.theme.TreasureTheme
import com.example.treasure.ui.uiComponents.SystemRequirementsSection
import com.example.treasure.ui.viewModels.DetailViewModel

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

    Scaffold(contentWindowInsets = WindowInsets.navigationBars,
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
                    GameHeader(
                        bannerUrl = gameDetail.screenshots?.firstOrNull() ?: gameDetail.thumbnail,
                        thumbnailUrl = gameDetail.thumbnail
                    )

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = gameDetail.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

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

                        // --- READ MORE LOGIC ---
                        var isExpanded by remember { mutableStateOf(false) }
                        var showReadMore by remember { mutableStateOf(false) }

                        Text(
                            text = gameDetail.description ?: "No description available.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 4.dp),
                            onTextLayout = { textLayoutResult ->
                                if (!isExpanded && textLayoutResult.hasVisualOverflow) {
                                    showReadMore = true
                                }
                            }
                        )

                        if (showReadMore) {
                            Text(
                                text = if (isExpanded) stringResource(R.string.read_less) else stringResource(R.string.read_more),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { isExpanded = !isExpanded }
                            )
                        }

                        if (!gameDetail.screenshots.isNullOrEmpty() || gameDetail.trailerUrl != null) {
                            LazyRow(
                                contentPadding = PaddingValues(top = 24.dp, bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Trailer Item
                                if (gameDetail.trailerUrl != null) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .width(280.dp)
                                                .aspectRatio(16f / 9f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { showVideoPlayer = true },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = gameDetail.thumbnail,
                                                contentDescription = "Trailer thumbnail",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                                                error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.3f))
                                            )
                                            Icon(
                                                imageVector = Icons.Default.PlayCircle,
                                                contentDescription = "Play trailer",
                                                modifier = Modifier.size(64.dp),
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }

                                // Screenshots
                                itemsIndexed(gameDetail.screenshots ?: emptyList()) { index, screenshotUrl ->
                                    AsyncImage(
                                        model = screenshotUrl,
                                        contentDescription = null,
                                        placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                                        error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier
                                            .width(280.dp)
                                            .aspectRatio(16f / 9f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { selectedScreenshotIndex = index },
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }

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

            // Full Screen Image Overlay with Horizontal Swiping
            selectedScreenshotIndex?.let { index ->
                FullScreenImageOverlay(
                    screenshots = gameDetail?.screenshots ?: emptyList(),
                    initialIndex = index,
                    onDismiss = { selectedScreenshotIndex = null }
                )
            }

            // Video Player Overlay
            if (showVideoPlayer && gameDetail?.trailerUrl != null) {
                VideoPlayerOverlay(
                    trailerUrl = gameDetail!!.trailerUrl!!,
                    onDismiss = { showVideoPlayer = false }
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerOverlay(
    trailerUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val attributionContext = remember(context) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.createAttributionContext("media_player")
            } else {
                context
            }
        }

        DisposableEffect(Unit) {
            val window = activity?.window
            val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

            if (window != null) {
                val insetsController = WindowInsetsControllerCompat(window, window.decorView)
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
            }

            onDispose {
                activity?.requestedOrientation = originalOrientation

                if (window != null) {
                    val insetsController = WindowInsetsControllerCompat(window, window.decorView)
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }

        val exoPlayer = remember {
            ExoPlayer.Builder(attributionContext).build().apply {
                val mediaItem = MediaItem.fromUri(trailerUrl)
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                exoPlayer.release()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = true
                        setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close player",
                    tint = Color.White
                )
            }
        }
    }
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun FullScreenImageOverlay(
    screenshots: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { screenshots.size }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { it }
            ) { pageIndex ->
                AsyncImage(
                    model = screenshots[pageIndex],
                    contentDescription = "Full screen screenshot ${pageIndex + 1}",
                    modifier = Modifier.fillMaxSize().clickable { onDismiss() },
                    contentScale = ContentScale.Fit
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 48.dp, end = 16.dp)
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            if (screenshots.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${screenshots.size}",
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun GameHeader(
    bannerUrl: String?,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier
) {
    val bgColor = MaterialTheme.colorScheme.background

    Box(modifier = modifier.height(280.dp)) {
        // Banner Image
        AsyncImage(
            model = bannerUrl,
            contentDescription = null,
            placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
            error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentScale = ContentScale.Crop
        )

        // TOP GRADIENT: Protects the Top App Bar text and icon visibility
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp) // Height matches the typical top app bar area
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            bgColor.copy(alpha = 0.85f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Thumbnail Image
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
            error = ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .padding(start = 16.dp)
                .size(100.dp, 140.dp)
                .align(Alignment.BottomStart)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun DetailInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) {
                append("$label ")
            }
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                append(value)
            }
        },
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier.padding(vertical = 2.dp)
    )
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