package id.andriawan.cofinance.localization

import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.en
import cofinance.composeapp.generated.resources.id
import org.jetbrains.compose.resources.StringResource

enum class AppLang(
    val code: String,
    val stringRes: StringResource
) {
    English("en", Res.string.en),
    Indonesian("id", Res.string.id)
}