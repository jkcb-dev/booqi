package corp.khin.solutions.booqi.feature.browse

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.component.CategoryChip
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing

/**
 * C2's distance filter: "Cualquiera" and then [DISTANCE_OPTIONS_KM] (1/2/5/10 km). Tapping one
 * re-runs the search with that radius around the (TEMPORARY, see [TEMPORARY_CUSTOMER_LOCATION])
 * location.
 */
@Composable
internal fun DistanceFilter(radiusKm: Int?, onAction: (BrowseAction) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = BooqiSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.xs),
    ) {
        Text("Distancia", style = MaterialTheme.typography.labelLarge)
        CategoryChip(
            label = "Cualquiera",
            selected = radiusKm == null,
            onClick = { onAction(BrowseAction.DistanceSelected(null)) },
        )
        DISTANCE_OPTIONS_KM.forEach { km ->
            CategoryChip(
                label = "$km km",
                selected = radiusKm == km,
                onClick = { onAction(BrowseAction.DistanceSelected(km)) },
            )
        }
    }
}
