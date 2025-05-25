package id.andriawan24.cofinance.domain.model.response

data class Account(
    val label: String,
    val group: String
) {
    companion object {
        fun dummy() = listOf(
            Account("Allowance", "Cash"),
            Account("BCA", "Debit Card"),
            Account("BNI", "Debit Card"),
            Account("BRI", "Debit Card"),
            Account("Mandiri", "Debit Card"),
        )
    }
}
