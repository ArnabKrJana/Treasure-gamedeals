package com.example.treasure.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.treasure.R
import com.example.treasure.ui.theme.TreasureTheme
import com.example.treasure.ui.viewModels.AuthViewModel

@Composable
fun WelcomeScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current

    // Observe errors and show a Toast automatically
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    WelcomeScreenContent(
        isLoading = isLoading,
        onGoogleSignInClick = {
            // The UI only cares about delegating the click to the ViewModel
            viewModel.handleGoogleLogin(context, onNavigateToHome)
        }
    )
}

@OptIn(UnstableApi::class)
@Composable
fun WelcomeScreenContent(
    isLoading: Boolean,
    onGoogleSignInClick: () -> Unit
) {
    val context = LocalContext.current
    var showLoginCard by remember { mutableStateOf(false) }

    // BLUR ANIMATION RESTORED!
    val blurRadius by animateDpAsState(
        targetValue = if (showLoginCard) 24.dp else 0.dp,
        animationSpec = tween(durationMillis = 500),
        label = "blur_anim"
    )

    // Slight dimming to ensure the white Login card pops against bright videos
    val dimmingAlpha by animateFloatAsState(
        targetValue = if (showLoginCard) 0.4f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "dimming_anim"
    )

    // Initialize Random Background Video
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val videoOptions = listOf(R.raw.bg_vid_01)
            val randomVideo = videoOptions.random()

            val uri = Uri.parse("android.resource://${context.packageName}/$randomVideo")

            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f // Completely muted
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {

        // --- LAYER 1: VIDEO BACKGROUND ---
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Hide all controls
                    resizeMode =
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM // Fill screen like a wallpaper
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = blurRadius) // Blur is back in action!
        )

        // --- LAYER 1.5: SUBTLE DIMMING ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dimmingAlpha))
        )

        // --- LAYER 2: DARK GRADIENTS ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                    )
                )
        )

        // --- LAYER 3: WELCOME CONTENT (Hidden when login shows) ---
        AnimatedVisibility(
            visible = !showLoginCard,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 80.dp)
            ) {
                Text(
                    text = "TREASURE",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { showLoginCard = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Text(
                    text = "PRESS TO START",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

        // --- LAYER 4: LOGIN CARD (Slides up over video) ---
        AnimatedVisibility(
            visible = showLoginCard,
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(500)
            ) + fadeIn(tween(500)),
            exit = slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(500)
            ) + fadeOut(tween(500)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { showLoginCard = false }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(enabled = false) {} // Consume clicks inside the card
                        .padding(horizontal = 24.dp, vertical = 48.dp)
                        .padding(
                            bottom = WindowInsets.navigationBars.asPaddingValues()
                                .calculateBottomPadding()
                        )
                ) {
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Access your wishlist, sync your deals, and customize your experience.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )

                    Button(
                        onClick = onGoogleSignInClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onSurface,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Continue with Google",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { showLoginCard = false }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

/**
 * Click "Run Preview" in the gutter next to this function to run it directly on the emulator!
 */
@Preview(name = "Welcome Screen Emulator Test", showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    TreasureTheme {
        // By using WelcomeScreenContent here, we bypass Hilt completely!
        WelcomeScreenContent(
            isLoading = false,
            onGoogleSignInClick = {}
        )
    }
}