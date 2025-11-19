package id.andriawan24.cofinance.andro.ui.presentation.account.models

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import id.andriawan24.cofinance.domain.model.response.Account

data class AccountByGroup(
    val groupLabel: String,
    val totalAmount: Long,
    val backgroundColor: Color,
    @param:DrawableRes val imageRes: Int,
    val accounts: List<Account>
)
