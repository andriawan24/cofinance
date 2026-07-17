package id.andriawan.cofinance.domain.model.request

data class AccountParam(
    val name: String,
    val balance: Long,
    val group: String,
    val accountType: String = "REGULAR_BALANCE"
)
