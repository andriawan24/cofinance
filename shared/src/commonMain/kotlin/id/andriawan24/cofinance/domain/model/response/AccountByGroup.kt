package id.andriawan24.cofinance.domain.model.response

import id.andriawan24.cofinance.utils.enums.AccountGroupType

data class AccountByGroup(
    val groupLabel: String,
    val totalAmount: Long,
    val backgroundColor: Long,
    val accountGroupType: AccountGroupType,
    val accounts: List<Account>
)
