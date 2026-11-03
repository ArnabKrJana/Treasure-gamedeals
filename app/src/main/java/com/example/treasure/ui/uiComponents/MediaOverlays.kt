package com.example.treasure.ui.uiComponents

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.annotation.OptIn
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.example.treasure.R
import com.example.treasure.ui.theme.TreasureTheme

/**
 * Execution status state driving animated icon transitions for overlay media actions.
 */
enum class ActionStatus {
    /**
     * Resting default state ready for interaction.
     */
    IDLE,

    /**
     * Active asynchronous operation executing (renders circular progress indicator).
     */
    LOADING,

    /**
     * Successful completion state (renders checked icon with success tint).
     */
    SUCCESS
}

/**
 * Full-screen modal video player dialog composable powered by AndroidX Media3 ExoPlayer.
 *
 * Implements immersive video playback features:
 * - Temporarily locks device orientation to [ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE].
 * - Hides platform status and navigation system bars using [WindowInsetsControllerCompat].
 * - Releases the [ExoPlayer] instance cleanly via [DisposableEffect] when the overlay is dismissed.
 *
 * @param trailerUrl Remote streaming URL of the video trailer.
 * @param onDismiss Callback invoked when the user dismisses the dialog or taps the close button.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerOverlay(
    trailerUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val view = LocalView.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val attributionContext = remember(context) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && !view.isInEditMode) {
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

        val exoPlayer = if (!view.isInEditMode) {
            remember {
                ExoPlayer.Builder(attributionContext).build().apply {
                    val mediaItem = MediaItem.fromUri(trailerUrl)
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = true
                }
            }
        } else {
            null
        }

        DisposableEffect(Unit) {
            onDispose { exoPlayer?.release() }
        }

        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (exoPlayer != null) {
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
            } else {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = "Video Preview Placeholder",
                    modifier = Modifier.size(64.dp),
                    tint = Color.White.copy(alpha = 0.5f)
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close player", tint = Color.White)
            }
        }
    }
}

/**
 * Full-screen dialog wrapper presenting high-resolution game screenshots in an interactive viewer.
 *
 * @param screenshots List of remote screenshot image URLs.
 * @param initialIndex Starting page index displayed upon dialog presentation.
 * @param downloadStatuses Map tracking local download [ActionStatus] states per image URL.
 * @param uploadStatuses Map tracking Google Drive cloud sync [ActionStatus] states per image URL.
 * @param onDismiss Callback invoked when the user dismisses the dialog.
 * @param onDownloadLocalClick Callback invoked when requesting local storage download of the active screenshot.
 * @param onDriveSyncClick Callback invoked when requesting Google Drive cloud backup of the active screenshot.
 */
@Composable
fun FullScreenImageOverlay(
    screenshots: List<String>,
    initialIndex: Int,
    downloadStatuses: Map<String, ActionStatus>,
    uploadStatuses: Map<String, ActionStatus>,
    onDismiss: () -> Unit,
    onDownloadLocalClick: (String) -> Unit,
    onDriveSyncClick: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        FullScreenImageOverlayContent(
            screenshots = screenshots,
            initialIndex = initialIndex,
            downloadStatuses = downloadStatuses,
            uploadStatuses = uploadStatuses,
            onDismiss = onDismiss,
            onDownloadLocalClick = onDownloadLocalClick,
            onDriveSyncClick = onDriveSyncClick
        )
    }
}

/**
 * Content composable for the full-screen screenshot viewer dialog.
 *
 * Implements:
 * - [HorizontalPager] for swiping between high-resolution screenshots.
 * - Current page indicator chip (`"${currentPage + 1} / ${total}"`).
 * - Floating action pill with animated local download and Google Drive cloud sync buttons.
 *
 * @param screenshots List of screenshot URLs.
 * @param initialIndex Starting page index.
 * @param downloadStatuses Map of local download action states.
 * @param uploadStatuses Map of cloud sync action states.
 * @param onDismiss Dismiss callback.
 * @param onDownloadLocalClick Local download callback.
 * @param onDriveSyncClick Drive sync callback.
 */
@Composable
fun FullScreenImageOverlayContent(
    screenshots: List<String>,
    initialIndex: Int,
    downloadStatuses: Map<String, ActionStatus>,
    uploadStatuses: Map<String, ActionStatus>,
    onDismiss: () -> Unit,
    onDownloadLocalClick: (String) -> Unit,
    onDriveSyncClick: (String) -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { screenshots.size }
    val successColor = Color(0xFF4CAF50)

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { it }
        ) { pageIndex ->
            val interactionSource = remember { MutableInteractionSource() }

            AsyncImage(
                model = screenshots[pageIndex],
                contentDescription = "Full screen screenshot ${pageIndex + 1}",
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {},
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
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
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

        val currentUrl = screenshots.getOrNull(pagerState.currentPage) ?: ""
        val currentDownloadStatus = downloadStatuses[currentUrl] ?: ActionStatus.IDLE
        val currentUploadStatus = uploadStatuses[currentUrl] ?: ActionStatus.IDLE

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-20).dp, y = (-40).dp)
                .background(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .8f), shape = RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = { onDownloadLocalClick(currentUrl) },
                enabled = currentDownloadStatus == ActionStatus.IDLE,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Crossfade(targetState = currentDownloadStatus, label = "download_anim") { status ->
                    when (status) {
                        ActionStatus.LOADING -> CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        ActionStatus.SUCCESS -> Icon(imageVector = Icons.Default.DownloadDone, contentDescription = "Downloaded", tint = successColor)
                        ActionStatus.IDLE -> Icon(imageVector = Icons.Default.Download, contentDescription = "Save to device", tint = Color.White)
                    }
                }
            }

            IconButton(
                onClick = { onDriveSyncClick(currentUrl) },
                enabled = currentUploadStatus == ActionStatus.IDLE,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Crossfade(targetState = currentUploadStatus, label = "upload_anim") { status ->
                    when (status) {
                        ActionStatus.LOADING -> CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        ActionStatus.SUCCESS -> Icon(imageVector = Icons.Default.CloudDone, contentDescription = "Synced", tint = successColor)
                        ActionStatus.IDLE -> Icon(painter = painterResource(R.drawable.upload), contentDescription = "Sync to Drive", tint = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Traverses the [Context] hierarchy through [ContextWrapper] links to resolve the hosting [Activity].
 *
 * @return The hosting [Activity], or `null` if not found.
 */
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Preview(name = "Image Overlay", showBackground = true)
@Composable
fun FullScreenImageOverlayPreview() {
    TreasureTheme {
        FullScreenImageOverlayContent(
            screenshots = imageList,
            initialIndex = 0,
            downloadStatuses = mapOf(imageList.first() to ActionStatus.SUCCESS),
            uploadStatuses = mapOf(imageList.last() to ActionStatus.LOADING),
            onDismiss = {},
            onDriveSyncClick = {},
            onDownloadLocalClick = {}
        )
    }
}

val imageList = listOf(
    "https://images.unsplash.com/photo-1542806109-e88b46573e79",
    "https://images.unsplash.com/photo-1474511320723-9a56873867b5",
    "https://images.unsplash.com/photo-1544946632-b73cacef16ad"
)
