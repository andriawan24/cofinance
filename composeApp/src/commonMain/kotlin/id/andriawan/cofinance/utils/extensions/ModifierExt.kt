package id.andriawan.cofinance.utils.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier


@Composable
@Stable
fun Modifier.conditional(
    condition: Boolean,
    trueModifier: @Composable Modifier.() -> Modifier,
    falseModifier: (Modifier.() -> Modifier)? = null
): Modifier {
    return if (condition) {
        then(trueModifier(Modifier))
    } else {
        if (falseModifier != null) {
            then(falseModifier(Modifier))
        } else {
            this
        }
    }
}