package id.andriawan24.cofinance.andro.ui.presentation.stats.models

import id.andriawan24.cofinance.andro.utils.enums.TransactionCategory

data class StatItem(
    val category: TransactionCategory,
    val amount: Long,
    val percentage: Float,
    val sweepAngle: Float,
)
