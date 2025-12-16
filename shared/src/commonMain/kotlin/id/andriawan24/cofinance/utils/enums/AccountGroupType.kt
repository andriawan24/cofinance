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

        fun AccountGroupType.getBackgroundColor(): Long {
            return when (this) {
                CASH -> 0xFFEEF9F8
                DEBIT -> 0xFFEFFAFD
                CREDIT -> 0xFFEFFAFD
                SAVINGS -> 0xFFFFF4FD
            }
        }
    }
}