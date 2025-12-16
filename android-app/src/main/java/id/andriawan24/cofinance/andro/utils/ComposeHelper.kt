package id.andriawan24.cofinance.andro.utils

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.domain.model.response.AccountByGroup
import id.andriawan24.cofinance.utils.enums.AccountGroupType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
fun <T> Flow<T>.CollectAsEffect(block: (T) -> Unit) {
    LaunchedEffect(this) {
        onEach(block)
            .collect()
    }
}

fun AccountGroupType.toDrawable(): Int {
    return when (this) {
        AccountGroupType.CASH -> R.drawable.ic_money
        AccountGroupType.DEBIT -> R.drawable.ic_card
        AccountGroupType.CREDIT -> R.drawable.ic_card
        AccountGroupType.SAVINGS -> R.drawable.ic_saving
    }
}