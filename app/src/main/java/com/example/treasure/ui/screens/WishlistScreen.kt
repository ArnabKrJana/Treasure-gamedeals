package com.example.treasure.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.treasure.data.local.entity.UserInteractionEntity
import com.example.treasure.domain.uiModels.GameCardItem
import com.example.treasure.domain.uiModels.Price
import com.example.treasure.ui.uiComponents.GameCard
import com.example.treasure.ui.viewModels.WishlistViewModel

@Composable
fun WishlistScreen(
    viewModel: WishlistViewModel = hiltViewModel(),
    onItemClick: (String) -> Unit
) {

    val wishlistItems by viewModel.wishlistItems.collectAsStateWithLifecycle()

    WishlistScreenContent(
        wishlistItems = wishlistItems,
        onToggleFavorite = { viewModel.toggleFavorite(it) },
        onItemClick = onItemClick
    )
}

@Composable
fun WishlistScreenContent(
    wishlistItems: List<UserInteractionEntity>,
    onToggleFavorite: (UserInteractionEntity) -> Unit,
    onItemClick: (String) -> Unit
) {

    val gridState = rememberLazyGridState()


    if (wishlistItems.isEmpty()) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = "Your wishlist is empty",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )
        }

    } else {

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {

            val columns =
                if (maxWidth < 600.dp) 2
                else (maxWidth / 180.dp).toInt()

            LazyVerticalGrid(

                state = gridState,

                columns = GridCells.Fixed(columns),

                modifier = Modifier.fillMaxSize(),

                contentPadding = PaddingValues(
                  16.dp,
                ),

                horizontalArrangement = Arrangement.spacedBy(12.dp),

                verticalArrangement = Arrangement.spacedBy(12.dp)


            ) {

                items(
                    items = wishlistItems,
                    key = { it.gameId }
                ) { entity ->

                    GameCard(

//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .aspectRatio(0.68f),

                        game = entity.toGameCardItem(),

                        isScrolling = gridState.isScrollInProgress,

                        isFavorite = entity.isFavorite,

                        onToggleFavorite = { onToggleFavorite(entity) },

                        onItemClick = onItemClick
                    )
                }
            }
        }
    }
}

private fun UserInteractionEntity.toGameCardItem(): GameCardItem {

    val displayCurrent = latestSyncedPrice ?: currentPrice

    val displayOriginal =
        if (latestSyncedPrice != null) currentPrice
        else originalPrice

    return GameCardItem(
        id = gameId,
        listingIndex = 0,
        title = title,
        thumbnail = thumbnail,
        store = storeId,
        price = Price(
            originalPrice = displayOriginal,
            currentPrice = displayCurrent
        ),
        upVotes = null
    )
}

@Preview(
    name = "Phone - Light",
    widthDp = 360,
    heightDp = 800
)
@Preview(
    name = "Phone - Dark",
    widthDp = 360,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    name = "Tablet",
    widthDp = 800,
    heightDp = 1280
)
@Preview(
    name = "Tablet - Dark",
    widthDp = 800,
    heightDp = 1280,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun WishlistScreenPreview() {

    val sampleWishlist = listOf(

        UserInteractionEntity(
            gameId = "1",
            title = "The Witcher 3: Wild Hunt",
            thumbnail = null,
            currentPrice = 250.0,
            originalPrice = 1000.0,
            storeId = "Steam",
            isFavorite = true
        ),

        UserInteractionEntity(
            gameId = "2",
            title = "Cyberpunk 2077",
            thumbnail = null,
            currentPrice = 1499.0,
            originalPrice = 2999.0,
            storeId = "GOG",
            isFavorite = true
        ),

        UserInteractionEntity(
            gameId = "3",
            title = "Red Dead Redemption 2",
            thumbnail = null,
            currentPrice = 1055.0,
            originalPrice = 3199.0,
            storeId = "Epic",
            isFavorite = true
        ),

        UserInteractionEntity(
            gameId = "4",
            title = "Elden Ring",
            thumbnail = null,
            currentPrice = 2499.0,
            originalPrice = 2499.0,
            storeId = "Steam",
            isFavorite = true
        )
    )

    MaterialTheme {
        Surface {
            WishlistScreenContent(
                wishlistItems = sampleWishlist,
                onToggleFavorite = {},
                onItemClick = {}
            )
        }
    }
}

@Preview(name = "Empty State")
@Composable
fun WishlistScreenEmptyPreview() {

    MaterialTheme {

        Surface {

            WishlistScreenContent(
                wishlistItems = emptyList(),
                onToggleFavorite = {},
                onItemClick = {}
            )
        }
    }
}