package id.andriawan.cofinance.domain.model.response

import id.andriawan.cofinance.utils.enums.AccountGroupType

data class AccountByGroup(
    val groupLabel: String,
    val totalAmount: Long,
    val backgroundColor: Long,
    val accountGroupType: AccountGroupType,
    val accounts: List<Account>
)
