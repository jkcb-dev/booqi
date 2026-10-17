package corp.khin.solutions.booqi.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiCornerRadius
import corp.khin.solutions.booqi.core.designsystem.theme.BooqiSpacing
import corp.khin.solutions.booqi.core.designsystem.theme.LocalBooqiExtendedColors
import corp.khin.solutions.booqi.domain.model.SocialProvider

/** A screen body: padded, scrollable and spaced, under a [title] heading that every state keeps. */
@Composable
internal fun AccountScreenBody(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(BooqiSpacing.md),
        verticalArrangement = Arrangement.spacedBy(BooqiSpacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            trailing?.invoke()
        }
        content()
    }
}

/** The "Volver"-style text button at the end of a heading. */
@Composable
internal fun HeadingAction(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(label) }
}

@Composable
internal fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxWidth().padding(BooqiSpacing.lg), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

/** A load failure under the heading, with the way to try again. */
@Composable
internal fun LoadFailure(message: String, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.sm)) {
        ErrorText(message)
        OutlinedButton(onClick = onRetry) { Text("Reintentar") }
    }
}

@Composable
internal fun ErrorText(message: String) {
    Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}

@Composable
internal fun SecondaryText(text: String, modifier: Modifier = Modifier, align: TextAlign = TextAlign.Start) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = LocalBooqiExtendedColors.current.ink2,
        textAlign = align,
        modifier = modifier,
    )
}

/** What an [AccountTextField] holds: decides its keyboard and whether the text is masked. */
internal enum class AccountFieldKind { Text, Email, Password, Url }

/** Single-line field of the account forms with an inline [errorMessage]. */
@Composable
internal fun AccountTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    errorMessage: String? = null,
    kind: AccountFieldKind = AccountFieldKind.Text,
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
        keyboardOptions = KeyboardOptions(keyboardType = kind.keyboardType()),
        visualTransformation = if (kind == AccountFieldKind.Password) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        singleLine = true,
    )
}

/**
 * "Continuar con Google / Facebook / Apple". They run the sign-in against the fake account source;
 * the real provider SDKs (consent screens) are Platform Integration's job, and swapping them in
 * changes the data layer, not these buttons.
 */
@Composable
internal fun SocialSignInButtons(enabled: Boolean, onClick: (SocialProvider) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(BooqiSpacing.xs)) {
        SocialProvider.entries.forEach { provider ->
            OutlinedButton(
                onClick = { onClick(provider) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(provider.buttonLabel()) }
        }
    }
}

/**
 * Stand-in for a user's photo: the first letter of the name on a brand tint. There is no image
 * loader in the project yet (choosing one is Architect's call), so `photoUrl`s are not rendered;
 * every photo slot goes through here so swapping in real images touches one place.
 */
@Composable
internal fun UserAvatar(name: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(BooqiSpacing.xxxl)
            .clip(RoundedCornerShape(BooqiCornerRadius.pill))
            .background(MaterialTheme.colorScheme.primaryContainer),
    ) {
        Text(
            text = name.trim().take(1).uppercase(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

private fun AccountFieldKind.keyboardType(): KeyboardType = when (this) {
    AccountFieldKind.Text -> KeyboardType.Text
    AccountFieldKind.Email -> KeyboardType.Email
    AccountFieldKind.Password -> KeyboardType.Password
    AccountFieldKind.Url -> KeyboardType.Uri
}
