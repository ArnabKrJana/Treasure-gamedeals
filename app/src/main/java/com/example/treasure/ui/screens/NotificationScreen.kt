package com.example.treasure.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

/**
 * Stateful Composable screen displaying price drop notifications and discount alerts.
 *
 * @param viewModel Injected [NotificationViewModel] providing reactive notification records.
 * @param onBackClick Invoked when the back navigation button is tapped.
 */
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

/**
 * Stateless presentation container for notifications, supporting swipe-to-dismiss and bulk deletion.
 *
 * @param notifications List of persisted [NotificationEntity] alert records.
 * @param onClearAll Callback invoked to clear all notifications from persistence.
 * @param onDelete Callback invoked to remove an individual notification by ID.
 * @param onBackClick Callback invoked to navigate back.
 */
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (notifications.isNotEmpty()) {
                        IconButton(onClick = onClearAll) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All")
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
                                label = "dismiss_color"
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
                                    contentDescription = "Delete",
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

/**
 * Individual row item presenting game title, old price, new discount price, and thumbnail.
 *
 * @param item The [NotificationEntity] record to render.
 */
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

/**
 * Preview Composable for [NotificationScreenContent] displaying populated alert records.
 */
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

/**
 * Preview Composable for [NotificationScreenContent] in empty state.
 */
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
