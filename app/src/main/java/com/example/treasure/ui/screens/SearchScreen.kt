package com.example.treasure.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.treasure.domain.uiModels.GameCardItem
import com.example.treasure.domain.uiModels.Price
import com.example.treasure.domain.uiModels.UpVotes
import com.example.treasure.ui.theme.LocalThemeIsDark
import com.example.treasure.ui.viewModels.SearchViewModel
import com.example.treasure.utils.ColorCode

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onItemClick: (String) -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

    SearchScreenContent(
        searchQuery = searchQuery,
        searchResults = searchResults,
        isSearching = isSearching,
        onQueryChange = { viewModel.onQueryChange(it) },
        onSearchTriggered = { viewModel.performSearch() }, // 1. Pass the search trigger down
        onBackClick = onBackClick,
        onItemClick = onItemClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreenContent(
    searchQuery: String,
    searchResults: List<GameCardItem>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSearchTriggered: () -> Unit, // 2. Receive the trigger
    onBackClick: () -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var active by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            SearchBar(
                query = searchQuery,
                onQueryChange = onQueryChange,
                onSearch = {
                    active = false
                    onSearchTriggered() // 3. Fire when Keyboard "Enter/Search" is pressed
                },
                active = active,
                onActiveChange = { active = it },
                placeholder = { Text("Search Games") },
                leadingIcon = {
                    IconButton(onClick = {
                        if (active) active = false else onBackClick()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Row {
                            // 4. Fire when the visual Search icon is clicked
                            IconButton(onClick = {
                                active = false
                                onSearchTriggered()
                            }) {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                            }
                            // Clear button
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (active) 0.dp else 16.dp),
                colors = SearchBarDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Suggestions", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Action games",
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .clickable {
                                onQueryChange("Action")
                                active = false
                                onSearchTriggered() // 5. Fire when suggestion is clicked
                            }
                    )
                    Text(
                        "RPG",
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .clickable {
                                onQueryChange("RPG")
                                active = false
                                onSearchTriggered() // 5. Fire when suggestion is clicked
                            }
                    )
                }
            }
        }
    ) { paddingValues ->
        if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(searchResults) { game ->
                    SearchGameCard(
                        game = game,
                        onClick = {
                            active = false
                            onItemClick(game.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchGameCard(
    game: GameCardItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalThemeIsDark.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .height(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            if (game.thumbnail != null) {
                AsyncImage(
                    model = game.thumbnail,
                    contentDescription = game.title,
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier.fillMaxSize(),
                    placeholder = ColorPainter(Color.Gray),
                    error = ColorPainter(Color.Gray)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent,Color.Black.copy(0.7f), Color.Black)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = game.title,
                    color =Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp
                )

                if (game.genres.isNotEmpty()) {
                    Text(
                        text = game.genres.first(),
                        color = Color(0xFFFF5252),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                game.price?.let {
                    Text(
                        text = "₹${it.currentPrice.toInt()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if(!isDark) Color(0xff00B7B5) else Color(0xFF00FF00)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SearchScreenPreview() {
    SearchScreenContent(
        searchQuery = "",
        searchResults = emptyList(),
        isSearching = false,
        onQueryChange = {},
        onSearchTriggered = {},
        onBackClick = {},
        onItemClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun SearchGameCardPreview() {
    val dummyGame = GameCardItem(
        id = "1",
        listingIndex = 0,
        title = "God of War Ragnarök",
        thumbnail = "https://example.com/image.jpg",
        store = "Steam",
        upVotes = UpVotes("95%", ColorCode.GREEN),
        price = Price(originalPrice = 4999.0, currentPrice = 2999.0),
        genres = listOf("Action", "Adventure")
    )

    Column(modifier = Modifier.padding(16.dp)) {
        SearchGameCard(
            game = dummyGame,
            onClick = {}
        )
    }
}