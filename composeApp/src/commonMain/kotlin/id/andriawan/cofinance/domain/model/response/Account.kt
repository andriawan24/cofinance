package id.andriawan.cofinance.domain.model.response

import id.andriawan.cofinance.data.model.response.AccountResponse
import id.andriawan.cofinance.utils.enums.AccountGroupType

data class Account(
    val id: String = "",
    val name: String = "",
    val group: AccountGroupType = AccountGroupType.CASH,
    val balance: Long = 0,
    val createdAt: String = ""
) {
    companion object {
        fun from(response: AccountResponse?): Account {
            return Account(
                id = response?.id.orEmpty(),
                name = response?.name.orEmpty(),
                group = AccountGroupType.getAccountByName(response?.group.orEmpty()),
                balance = response?.balance ?: 0,
                createdAt = response?.createdAt.orEmpty()
            )
        }
    }
}
