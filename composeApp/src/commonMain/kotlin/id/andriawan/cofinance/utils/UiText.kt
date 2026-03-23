package id.andriawan.cofinance.utils

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

sealed class UiText {
    data class Raw(val value: String) : UiText()
    data class Res(val id: StringResource) : UiText()

    @Composable
    fun asString(): String = when (this) {
        is Raw -> value
        is Res -> stringResource(id)
    }
}
