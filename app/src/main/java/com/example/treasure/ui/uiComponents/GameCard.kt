package com.example.treasure.ui.uiComponents

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.treasure.domain.uiModels.GameCardItem
import com.example.treasure.domain.uiModels.Price
import com.example.treasure.domain.uiModels.UpVotes
import com.example.treasure.utils.ColorCode
import com.example.treasure.utils.helper.getDynamicColor

@Composable
fun GameCard(
    game: GameCardItem,
    isScrolling: Boolean = false,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    val defaultColor = MaterialTheme.colorScheme.surfaceVariant
    var cardColor by remember(game.thumbnail) { mutableStateOf(defaultColor) }

    val animatedColor by animateColorAsState(
        targetValue = cardColor,
        animationSpec = tween(500),
        label = "Color Fade"
    )

    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            animatedColor,
            animatedColor,
            if (isDark) Color(0xFF363636) else Color.LightGray
        )
    )

    LaunchedEffect(key1 = game.thumbnail, key2 = isScrolling) {
        if (!isScrolling) {
            cardColor = getDynamicColor(
                context = context,
                imageUrl = game.thumbnail,
                fallbackColor = Color.LightGray,
                isDark = isDark
            )
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.65f)
            .padding(4.dp)
            .clickable { onItemClick(game.id) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
        ) {

            val smallCard = maxWidth < 180.dp

            val titleSize = if (smallCard) 12.sp else 14.sp
            val priceSize = if (smallCard) 14.sp else 16.sp
            val originalPriceSize = if (smallCard) 10.sp else 12.sp
            val discountSize = if (smallCard) 9.sp else 10.sp
            val iconSize = if (smallCard) 20.dp else 24.dp
            val padding = if (smallCard) 6.dp else 8.dp

            Column(modifier = Modifier.fillMaxSize()) {

                // IMAGE SECTION
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.55f)
                ) {

                    AsyncImage(
                        model = game.thumbnail,
                        contentDescription = game.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(
                                RoundedCornerShape(
                                    bottomStart = 15.dp,
                                    bottomEnd = 15.dp
                                )
                            )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Badge(
                            text = game.store,
                            color = Color.White,
                            textColor = Color.Black
                        )
                    }
                }

                // DETAILS SECTION
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.45f)
                        .padding(horizontal = padding, vertical = padding),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(
                        text = game.title,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp,
                        fontSize = titleSize
                    )

                    game.price?.let { price ->

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            Text(
                                text = "₹${price.currentPrice.toInt()}",
                                color = if (!isDark) Color(0xff00B7B5) else Color(0xFF00FF00),
                                fontWeight = FontWeight.Bold,
                                fontSize = priceSize,
                                maxLines = 1
                            )

                            Text(
                                text = "₹${price.originalPrice.toInt()}",
                                color = if (!isDark) Color.DarkGray else Color.LightGray,
                                fontSize = originalPriceSize,
                                textDecoration = TextDecoration.LineThrough,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            if (price.discountPercent > 0) {
                                Text(
                                    text = "-${price.discountPercent}%",
                                    color = Color.White,
                                    fontSize = discountSize,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(Color(0xFFD90429))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                    } ?: Spacer(modifier = Modifier.weight(1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(iconSize)
                        ) {

                            Icon(
                                imageVector =
                                    if (isFavorite)
                                        Icons.Filled.Favorite
                                    else
                                        Icons.Default.FavoriteBorder,
                                contentDescription = "Like",
                                tint =
                                    if (isFavorite)
                                        Color(0xFFEB0076)
                                    else
                                        Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Badge(
    text: String,
    color: Color,
    textColor: Color = Color.White,
    modifier: Modifier = Modifier
) {

    Text(
        text = text.uppercase().substringBefore(" "),
        color = textColor,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(color)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Preview(name = "Phone")
@Preview(name = "Tablet", widthDp = 600)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun GameCardPreview() {

    val sampleGame = GameCardItem(
        id = "1",
        listingIndex = 1,
        title = "The Witcher 3: Wild Hunt - Complete Edition",
        thumbnail = null,
        store = "Steam",
        upVotes = UpVotes("95%", ColorCode.GREEN),
        price = Price(1000.0, 250.0),
        genres = listOf("RPG", "Open World")
    )

    MaterialTheme {
        Surface {
            GameCard(
                game = sampleGame,
                isFavorite = true,
                onToggleFavorite = {},
                onItemClick = {}
            )
        }
    }
}