package id.andriawan.cofinance.domain.model.response

import androidx.compose.runtime.Stable
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.ic_card
import cofinance.composeapp.generated.resources.ic_money
import cofinance.composeapp.generated.resources.ic_saving
import id.andriawan.cofinance.utils.enums.AccountGroupType
import org.jetbrains.compose.resources.DrawableResource

@Stable
data class AccountByGroup(
    val groupLabel: String,
    val totalAmount: Long,
    val backgroundColor: Long,
    val accountGroupType: AccountGroupType,
    val accounts: List<Account>
)

fun AccountGroupType.toDrawable(): DrawableResource {
    return when (this) {
        AccountGroupType.CASH -> Res.drawable.ic_money
        AccountGroupType.DEBIT -> Res.drawable.ic_card
        AccountGroupType.CREDIT -> Res.drawable.ic_card
        AccountGroupType.SAVINGS -> Res.drawable.ic_saving
    }
}
