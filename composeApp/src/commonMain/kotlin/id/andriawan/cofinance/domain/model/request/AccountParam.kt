package id.andriawan.cofinance.domain.model.request

import id.andriawan.cofinance.data.model.request.AccountRequest

data class AccountParam(
    val name: String,
    val balance: Long,
    val group: String,
    val accountType: String = "REGULAR_BALANCE"
) {
    companion object {
        fun AccountParam.toRequest(): AccountRequest {
            return AccountRequest(
                name = this.name,
                balance = this.balance,
                group = this.group,
                accountType = this.accountType
            )
        }
    }
}
