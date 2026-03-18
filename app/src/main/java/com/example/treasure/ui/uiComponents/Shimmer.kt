package com.example.treasure.ui.uiComponents

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedShimmer(
    modifier: Modifier = Modifier
        .width(175.dp) // Default width for HomeScreen
        .aspectRatio(0.65f) // Matches GameCard aspect ratio
) {
    // 1. Define Colors (Neutral Grays for loading)
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    // 2. Setup Transition
    val transition = rememberInfiniteTransition(label = "Shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTranslate"
    )

    // 3. Create the Moving Brush
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    CardShimmerItem(brush = brush, modifier = modifier)
}

@Composable
fun CardShimmerItem(brush: Brush, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.padding(4.dp), // Matches GameCard padding
        shape = RoundedCornerShape(16.dp), // Matches GameCard radius
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- TOP: Image Placeholder (55% of card) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.55f)
                    .clip(
                        RoundedCornerShape(
                            bottomStart = 15.dp,
                            bottomEnd = 15.dp
                        )
                    )
                    .background(brush)
            ) {
                // Store Badge Placeholder
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .width(42.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        // Make it slightly darker/distinct from the main brush
                        .background(Color.Gray.copy(alpha = 0.3f))
                )
            }

            // --- BOTTOM: Details Placeholders (45% of card) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f)
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Title Lines (2 lines to mimic maxLines = 2)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .height(14.dp)
                            .fillMaxWidth(0.9f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                    Box(
                        modifier = Modifier
                            .height(14.dp)
                            .fillMaxWidth(0.6f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                }

                // Price Row Placeholder
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Current Price
                    Box(modifier = Modifier.height(16.dp).width(45.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                    // Original Price
                    Box(modifier = Modifier.height(12.dp).width(35.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                    Spacer(modifier = Modifier.weight(1f))
                    // Discount Badge Placeholder
                    Box(modifier = Modifier.height(16.dp).width(30.dp).clip(RoundedCornerShape(7.dp)).background(brush))
                }

                // Bottom Row (Favorite Icon)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(12.dp)) // Circular placeholder for the heart
                            .background(brush)
                    )
                }
            }
        }
    }
}

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun ShimmerPreview() {
    AnimatedShimmer()
}