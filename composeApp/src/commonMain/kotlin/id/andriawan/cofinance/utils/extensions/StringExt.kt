package id.andriawan.cofinance.utils.extensions

fun String.isDigitOnly(): Boolean {
    return this.matches("^\\d+$".toRegex())
}
