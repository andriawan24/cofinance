package id.andriawan24.cofinance.domain.model.request

import id.andriawan24.cofinance.data.model.request.AccountRequest

data class AccountParam(
    val name: String,
    val balance: Long,
    val group: String
) {
    companion object {
        fun AccountParam.toRequest(): AccountRequest {
            return AccountRequest(
                name = this.name,
                balance = this.balance,
                group = this.group
            )
        }
    }
}
