package id.andriawan.cofinance.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import id.andriawan.cofinance.utils.enums.TransactionType

object ColorHelper {
    @Composable
    @ReadOnlyComposable
    fun getColorByExpenseType(transactionType: TransactionType): Color {
        return when (transactionType) {
            TransactionType.EXPENSE -> Color(0xFFC50102)
            TransactionType.DRAFT -> Color(0xFFC50102)
            TransactionType.INCOME -> Color(0xFF045330)
            TransactionType.TRANSFER -> MaterialTheme.colorScheme.onBackground
        }
    }
}
