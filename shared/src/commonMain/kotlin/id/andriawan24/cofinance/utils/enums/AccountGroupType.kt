package id.andriawan24.cofinance.utils.enums

enum class AccountGroupType(val displayName: String) {
    CASH("Cash"),
    DEBIT("Debit"),
    CREDIT("Credit"),
    SAVINGS("Savings");

    companion object Companion {
        fun getAccountByName(name: String): AccountGroupType {
            return AccountGroupType.entries.firstOrNull { it.name == name } ?: CASH
        }
    }
}