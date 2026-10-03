package corp.khin.solutions.booqi.feature.provider

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius

/** Single-line field of the P5 form, styled like #13's `ProfileTextField` (which has no keyboard
 * type) with an inline [errorMessage] and a [keyboardType] for the numeric price/duration fields. */
@Composable
internal fun ServiceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = errorMessage != null,
        supportingText = errorMessage?.let { message ->
            { Text(text = message, color = MaterialTheme.colorScheme.error) }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BooqiCornerRadius.small),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
    )
}
