package id.andriawan24.cofinance.domain.model.response

import id.andriawan24.cofinance.data.model.response.AccountResponse

data class Account(
    val id: String = "",
    val name: String = "",
    val group: String = "",
    val balance: Int = 0,
    val createdAt: String = ""
) {
    companion object {
        fun from(response: AccountResponse?): Account {
            return Account(
                id = response?.id.orEmpty(),
                name = response?.name.orEmpty(),
                group = response?.group.orEmpty(),
                balance = response?.balance ?: 0,
                createdAt = response?.createdAt.orEmpty()
            )
        }
    }
}
