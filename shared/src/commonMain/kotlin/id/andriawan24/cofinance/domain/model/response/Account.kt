package id.andriawan24.cofinance.domain.model.response

import id.andriawan24.cofinance.data.model.response.AccountResponse

data class Account(
    val id: Int,
    val name: String,
    val group: String,
    val balance: Int,
    val usersId: String,
    val createdAt: String
) {
    companion object {
        fun from(response: AccountResponse): Account {
            return Account(
                id = response.id ?: 0,
                name = response.name.orEmpty(),
                group = response.group.orEmpty(),
                balance = response.balance ?: 0,
                usersId = response.usersId.orEmpty(),
                createdAt = response.createdAt.orEmpty()
            )
        }
    }
}
