package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius

/** A time-of-day text field (`HH:mm`, see [parseTimeInput]) shared by the P6 day rows and the P7
 * "bloquear horas" form. Shows only the error *state*: the message itself is rendered once per
 * row/form by the caller, under the pair of fields. */
@Composable
internal fun ScheduleTimeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text("HH:mm") },
        isError = isError,
        modifier = modifier,
        shape = RoundedCornerShape(BooqiCornerRadius.small),
        singleLine = true,
    )
}
