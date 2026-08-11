package id.andriawan.cofinance.data.ocr.parser

private const val SEPARATOR = ","

/** Compact navigation-argument form of a field set: comma-joined enum names, or null when empty. */
fun Set<ReceiptField>.encodeToArgument(): String? =
    takeIf { it.isNotEmpty() }?.joinToString(SEPARATOR) { it.name }

/** Inverse of [encodeToArgument]. Unknown or malformed names are dropped. */
fun decodeReceiptFields(argument: String?): Set<ReceiptField> =
    argument.orEmpty()
        .split(SEPARATOR)
        .mapNotNullTo(mutableSetOf()) { name ->
            ReceiptField.entries.firstOrNull { it.name == name.trim() }
        }
