package corp.khin.solutions.booqi.feature.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors
import corp.khin.solutions.booqi.domain.model.Rating

/**
 * C12's star selector: five tappable stars, filled up to [stars] (0 = none chosen). Same glyph and
 * colors as `RatingDisplay`'s read-only row; each star is a radio-like control with a spoken
 * "N estrellas" label and a minimum touch target.
 */
@Composable
internal fun StarSelector(stars: Int, enabled: Boolean, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val filled = MaterialTheme.colorScheme.secondary
    val empty = LocalBooqiExtendedColors.current.ink3
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(BooqiSpacing.xxs)) {
        for (value in Rating.MIN_STARS..Rating.MAX_STARS) {
            Text(
                text = "★",
                style = MaterialTheme.typography.displaySmall,
                color = if (value <= stars) filled else empty,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .semantics { contentDescription = if (value == 1) "1 estrella" else "$value estrellas" }
                    .selectable(
                        selected = value == stars,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onSelect(value) },
                    ),
            )
        }
    }
}
