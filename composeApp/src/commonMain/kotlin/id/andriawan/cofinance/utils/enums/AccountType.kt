package id.andriawan.cofinance.utils.enums

enum class AccountType(val displayName: String) {
    ASSET("Asset"),
    REGULAR_BALANCE("Regular Balance");

    companion object {
        fun fromName(name: String): AccountType {
            return entries.firstOrNull { it.name == name } ?: REGULAR_BALANCE
        }
    }
}
