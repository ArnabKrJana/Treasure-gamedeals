package com.example.treasure.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.treasure.data.local.entity.NotificationEntity
import com.example.treasure.ui.viewModels.NotificationViewModel

@Composable
fun NotificationScreen(
    viewModel: NotificationViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {

    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    NotificationScreenContent(
        notifications = notifications,
        onClearAll = { viewModel.clearAll() },
        onDelete = { viewModel.deleteNotification(it) },
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreenContent(
    notifications: List<NotificationEntity>,
    onClearAll: () -> Unit,
    onDelete: (Int) -> Unit,
    onBackClick: () -> Unit
) {

    Scaffold(
        topBar = {

            TopAppBar(
                title = { Text("Notifications") },

                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },

                actions = {
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = onClearAll) {
                            Icon(Icons.Default.DeleteSweep, "Clear All")
                        }
                    }
                }
            )
        }
    ) { padding ->

        if (notifications.isEmpty()) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),

                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "No notifications",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

        } else {

            LazyColumn(

                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),

                contentPadding = PaddingValues(vertical = 8.dp)

            ) {

                items(
                    items = notifications,
                    key = { it.id }
                ) { item ->

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                onDelete(item.id)
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,

                        backgroundContent = {

                            val color by animateColorAsState(
                                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart)
                                    Color.Red.copy(alpha = 0.9f)
                                else
                                    Color.Transparent,
                                label = ""
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color)
                                    .padding(horizontal = 20.dp),

                                contentAlignment = Alignment.CenterEnd
                            ) {

                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                    ) {

                        NotificationItem(item)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(item: NotificationEntity) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )

            Spacer(Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "₹${item.oldPrice.toInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = TextDecoration.LineThrough
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "₹${item.newPrice.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF00B7B5),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        AsyncImage(
            model = item.thumbnail,
            contentDescription = null,

            modifier = Modifier
                .size(width = 120.dp, height = 68.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Gray),

            contentScale = ContentScale.Crop
        )
    }
}

@Preview(name = "Phone - Light", widthDp = 360, heightDp = 800)
@Preview(name = "Phone - Dark", widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tablet - Light", widthDp = 800, heightDp = 1280)
@Preview(name = "Tablet - Dark", widthDp = 800, heightDp = 1280, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun NotificationPreview() {

    val now = System.currentTimeMillis()

    val list = listOf(
        NotificationEntity(
            id = 1,
            gameId = "1",
            title = "Price drop on Elden Ring",
            thumbnail = null,
            oldPrice = 3999.0,
            newPrice = 2499.0,
            timestamp = now
        ),
        NotificationEntity(
            id = 2,
            gameId = "2",
            title = "Cyberpunk 2077 is now on Sale!",
            thumbnail = null,
            oldPrice = 2999.0,
            newPrice = 1499.0,
            timestamp = now
        ),
        NotificationEntity(
            id = 3,
            gameId = "3",
            title = "The Witcher 3: Wild Hunt price slashed",
            thumbnail = null,
            oldPrice = 1000.0,
            newPrice = 250.0,
            timestamp = now
        )
    )

    MaterialTheme {
        Surface {
            NotificationScreenContent(
                notifications = list,
                onClearAll = {},
                onDelete = {},
                onBackClick = {}
            )
        }
    }
}

@Preview(name = "Empty State - Light")
@Preview(name = "Empty State - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun NotificationEmptyPreview() {
    MaterialTheme {
        Surface {
            NotificationScreenContent(
                notifications = emptyList(),
                onClearAll = {},
                onDelete = {},
                onBackClick = {}
            )
        }
    }
}
