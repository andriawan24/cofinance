package id.andriawan.cofinance.components

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.label_needs_verification
import id.andriawan.cofinance.utils.Dimensions
import org.jetbrains.compose.resources.stringResource

private const val OUTLINE_ALPHA = 0.4f

/**
 * Marks a field a receipt scan filled in with low confidence. The value is a usable guess,
 * not an error, so the cue stays quiet: a hairline accent outline paired with
 * [NeedsVerificationHint], never an error style, and saving is never blocked.
 */
@Composable
fun Modifier.needsVerificationOutline(needsVerification: Boolean): Modifier =
    if (needsVerification) {
        border(
            width = Dimensions.SIZE_1,
            color = MaterialTheme.colorScheme.primary.copy(alpha = OUTLINE_ALPHA),
            shape = MaterialTheme.shapes.large
        )
    } else {
        this
    }

@Composable
fun NeedsVerificationHint(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = stringResource(Res.string.label_needs_verification),
        style = MaterialTheme.typography.labelSmall.copy(
            color = MaterialTheme.colorScheme.secondary
        )
    )
}
