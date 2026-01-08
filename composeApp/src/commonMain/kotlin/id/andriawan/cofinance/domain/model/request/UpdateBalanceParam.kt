package id.andriawan.cofinance.domain.model.request

import id.andriawan.cofinance.data.model.request.UpdateBalanceRequest

data class UpdateBalanceParam(
    val amount: Long,
    val accountsId: String,
    val usersId: String = ""
) {
    companion object Companion {
        fun UpdateBalanceParam.toRequest(): UpdateBalanceRequest {
            return UpdateBalanceRequest(
                amount = this.amount,
                accountsId = this.accountsId
            )
        }
    }
}
