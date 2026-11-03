package com.example.treasure.ui.uiComponents

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.treasure.R
import com.example.treasure.data.local.entity.SystemRequirementEntity
import com.example.treasure.domain.uiModels.Price
import com.example.treasure.domain.uiModels.RequirementType
import com.example.treasure.domain.uiModels.StoreDeal
import com.example.treasure.ui.theme.TreasureTheme
import java.util.Locale

/**
 * Renders the hardware and software specification requirements for a game, along with secondary
 * store purchase CTA buttons.
 *
 * Requirements are categorized into [RequirementType.MINIMUM] and [RequirementType.MAXIMUM] (recommended)
 * specs, formatted into distinct specification groups.
 *
 * @param modifier The layout modifier applied to the parent column container.
 * @param currentStore The display name of the primary vendor/storefront (e.g., "Steam").
 * @param currentPrice The pricing information for the primary store listing.
 * @param dealUrl The external deep link for the primary store listing (if applicable).
 * @param otherStores Collection of alternative store listings and active deal links.
 * @param systemRequirements Collection of hardware/software requirements persisted in Room.
 */
@Composable
fun SystemRequirementsSection(
    modifier: Modifier = Modifier,
    currentStore: String = "Steam",
    currentPrice: Price? = null,
    dealUrl: String? = null,
    otherStores: List<StoreDeal> = emptyList(),
    systemRequirements: List<SystemRequirementEntity>? = emptyList()
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        otherStores.filter { !it.dealUrl.isNullOrBlank() }.forEachIndexed { index, storeDeal ->
            BuyButton(
                storeName = storeDeal.storeName,
                price = storeDeal.price,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(storeDeal.dealUrl!!))
                    context.startActivity(intent)
                }
            )
            if (index < otherStores.size - 1) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.system_requirements),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        val minSpecs = systemRequirements?.filter { it.type == RequirementType.MINIMUM } ?: emptyList()
        val recSpecs = systemRequirements?.filter { it.type == RequirementType.MAXIMUM } ?: emptyList()

        if (minSpecs.isEmpty() && recSpecs.isEmpty()) {
            Text(
                text = "System requirements are not available yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            if (minSpecs.isNotEmpty()) {
                RequirementGroup(
                    title = stringResource(R.string.minimum),
                    titleColor = MaterialTheme.colorScheme.error,
                    requirements = minSpecs
                )
            }

            if (recSpecs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                RequirementGroup(
                    title = stringResource(R.string.recommended),
                    titleColor = MaterialTheme.colorScheme.error,
                    requirements = recSpecs
                )
            }
        }
    }
}

/**
 * Call-to-action button allowing the user to open an external store listing in the browser or vendor app.
 *
 * @param storeName Name of the storefront (e.g., "Steam", "GOG", "Epic Games").
 * @param price Pricing object containing base and current discount pricing.
 * @param onClick Invoked when the CTA button is tapped.
 * @param modifier The layout modifier applied to the button.
 */
@Composable
fun BuyButton(
    storeName: String,
    price: Price?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onSurface,
            contentColor = MaterialTheme.colorScheme.surface
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_menu_agenda),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.surface
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Buy on $storeName",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.surface
        )
        if (price != null) {
            Text(
                text = "₹${String.format(Locale.ROOT, "%.2f", price.currentPrice)}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = Color(0xFF4CAF50)
            )
        }
    }
}

/**
 * Renders a labeled section containing a sequence of system specification items (e.g., "Minimum" or "Recommended").
 *
 * @param title Header title of the requirement group.
 * @param titleColor Text color used for the group header title.
 * @param requirements The list of [SystemRequirementEntity] specifications belonging to this group.
 * @param modifier The layout modifier applied to the container.
 */
@Composable
fun RequirementGroup(
    title: String,
    titleColor: Color,
    requirements: List<SystemRequirementEntity>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            color = titleColor,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        requirements.forEach { req ->
            RequirementItem(label = req.specName, value = req.specValue)
        }
    }
}

/**
 * Key-value specification row displaying an individual hardware or software constraint.
 *
 * @param label The specification name (e.g., "OS", "Processor", "Memory").
 * @param value The specification value (e.g., "Windows 11 64-bit", "16 GB RAM").
 * @param modifier The layout modifier applied to the row.
 */
@Composable
fun RequirementItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Preview Composable illustrating the rendering of [SystemRequirementsSection] in light and dark modes.
 */
@Preview(showBackground = true)
@Composable
fun SystemRequirementsPreview() {
    val mockReqs = listOf(
        SystemRequirementEntity("OS", "Windows 10 64-bit", RequirementType.MINIMUM),
        SystemRequirementEntity("Processor", "Intel Core i5", RequirementType.MINIMUM),
        SystemRequirementEntity("Memory", "8 GB RAM", RequirementType.MINIMUM),
        SystemRequirementEntity("OS", "Windows 11", RequirementType.MAXIMUM),
        SystemRequirementEntity("Processor", "Intel Core i7", RequirementType.MAXIMUM)
    )

    TreasureTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            SystemRequirementsSection(
                dealUrl = "https://store.steampowered.com",
                currentPrice = Price(59.99, 29.99),
                otherStores = listOf(
                    StoreDeal("GOG", Price(59.99, 34.99), "https://gog.com"),
                    StoreDeal("Epic Games", Price(59.99, 29.99), "https://epicgames.com")
                ),
                systemRequirements = mockReqs
            )
        }
    }
}
