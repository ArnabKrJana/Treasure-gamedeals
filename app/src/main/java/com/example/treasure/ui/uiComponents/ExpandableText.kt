package com.example.treasure.ui.uiComponents

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.treasure.R

/**
 * Collapsible text block composable with automatic overflow detection and interactive "Read more" / "Read less" toggles.
 *
 * Utilizes `onTextLayout` to measure whether rendered lines visually exceed [minLines], displaying
 * the toggle button only when truncation occurs.
 *
 * @param text The full narrative or description text content to render.
 * @param modifier Composable modifier for outer container customization.
 * @param minLines Maximum visible lines when collapsed before truncation triggers. Defaults to 3.
 */
@Composable
fun ExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    minLines: Int = 3
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showReadMore by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (isExpanded) Int.MAX_VALUE else minLines,
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
    }
}
