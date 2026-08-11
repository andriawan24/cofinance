package id.andriawan.cofinance.data.ocr.parser

import id.andriawan.cofinance.utils.enums.TransactionCategory

/**
 * Keyword vocabulary of Indonesian receipts. Every entry is stored already normalized
 * by [normalizeForMatch] so lookups are whole-word rather than substring matches:
 * `TOTAL` must not fire on `SUBTOTAL`.
 */
internal object ReceiptLexicon {

    val TOTAL_KEYWORDS = listOf(
        "TOTAL",
        "GRAND TOTAL",
        "TOTAL BAYAR",
        "TOTAL BELANJA",
        "TOTAL PEMBAYARAN",
        "JUMLAH",
        "NOMINAL"
    )

    val NEGATIVE_KEYWORDS = listOf(
        "SUBTOTAL",
        "SUB TOTAL",
        "PPN",
        "PB1",
        "PAJAK",
        "KEMBALI",
        "KEMBALIAN",
        "TUNAI",
        "BAYAR TUNAI",
        "CASH",
        "DISKON"
    )

    val FEE_KEYWORDS = listOf(
        "BIAYA ADMIN",
        "BIAYA ADMINISTRASI",
        "BIAYA TRANSAKSI",
        "BIAYA LAYANAN",
        "ADMIN",
        "ADMINISTRASI",
        "FEE"
    )

    /** Merchant chains, checked before [ACQUIRERS] so a payment rail never masks the merchant. */
    val MERCHANTS: Map<String, TransactionCategory> = buildMap {
        putGroups(
            TransactionCategory.FOOD to listOf(
                "INDOMARET", "ALFAMART", "ALFAMIDI", "SUPERINDO", "HYPERMART", "TRANSMART",
                "RANCH MARKET", "FARMERS MARKET", "LOTTE MART", "GIANT", "CARREFOUR",
                "STARBUCKS", "KFC", "MCDONALDS", "MCDONALD", "MCD", "BURGER KING",
                "PIZZA HUT", "DOMINOS", "HOKBEN", "YOSHINOYA", "SOLARIA", "RICHEESE",
                "DUNKIN", "KRISPY KREME", "JCO", "CHATIME", "MIXUE", "KOPI KENANGAN",
                "JANJI JIWA", "FORE", "TOMORO", "POINT COFFEE", "BAKMI GM", "ES TELER 77",
                "GOFOOD", "GRABFOOD", "SHOPEEFOOD", "WARUNG", "RESTO", "RESTORAN"
            ),
            TransactionCategory.TRANSPORT to listOf(
                "GOJEK", "GORIDE", "GOCAR", "GRABBIKE", "GRABCAR", "GRAB", "MAXIM", "INDRIVE",
                "BLUEBIRD", "BLUE BIRD", "TRANSJAKARTA", "MRT JAKARTA", "MRT", "KRL",
                "KAI", "KERETA API", "DAMRI", "GARUDA INDONESIA", "LION AIR", "CITILINK",
                "AIRASIA", "PERTAMINA", "SPBU", "SHELL", "VIVO", "PARKIR", "TOL", "ETOLL",
                "E TOLL", "EMONEY", "E MONEY", "FLAZZ"
            ),
            TransactionCategory.APPAREL to listOf(
                "UNIQLO", "ZARA", "H M", "MATAHARI", "RAMAYANA", "ERIGO", "EIGER",
                "ADIDAS", "NIKE", "SPORT STATION", "PLANET SPORTS", "BERSHKA", "PULL BEAR"
            ),
            TransactionCategory.HEALTH to listOf(
                "KIMIA FARMA", "APOTEK", "APOTIK", "GUARDIAN", "WATSONS", "CENTURY",
                "HALODOC", "ALODOKTER", "KLINIK", "RUMAH SAKIT", "SILOAM", "PRODIA",
                "HERMINA", "MITRA KELUARGA", "BPJS"
            ),
            TransactionCategory.EDUCATION to listOf(
                "GRAMEDIA", "RUANGGURU", "ZENIUS", "SEKOLAH", "UNIVERSITAS", "KAMPUS",
                "SPP", "BIMBEL", "UDEMY", "COURSERA"
            ),
            TransactionCategory.SUBSCRIPTION to listOf(
                "NETFLIX", "SPOTIFY", "YOUTUBE PREMIUM", "DISNEY", "VIDIO", "VIU", "WETV",
                "IQIYI", "CANVA", "ADOBE", "APPLE", "ICLOUD", "GOOGLE ONE", "AMAZON PRIME"
            ),
            TransactionCategory.INTERNET to listOf(
                "TELKOMSEL", "INDOSAT", "IM3", "XL AXIATA", "AXIS", "SMARTFREN", "TRI",
                "INDIHOME", "BIZNET", "FIRST MEDIA", "MYREPUBLIC", "PULSA", "PAKET DATA"
            ),
            TransactionCategory.HOUSING to listOf(
                "PLN", "PDAM", "TOKEN LISTRIK", "LISTRIK", "IPL", "SEWA", "KONTRAKAN",
                "ACE HARDWARE", "INFORMA", "IKEA", "MITRA10", "DEPO BANGUNAN"
            ),
            TransactionCategory.DEBT to listOf(
                "KREDIVO", "AKULAKU", "HOME CREDIT", "INDODANA", "ADIRA", "PINJAMAN",
                "CICILAN", "ANGSURAN", "PAYLATER"
            ),
            TransactionCategory.OTHERS to listOf(
                "TOKOPEDIA", "SHOPEE", "LAZADA", "BUKALAPAK", "BLIBLI", "TIKTOK SHOP", "ZALORA"
            )
        )
    }

    /** Banks, e-wallets, and QRIS acquirers. Transfers and top-ups read as administration. */
    val ACQUIRERS: Map<String, TransactionCategory> = buildMap {
        putGroups(
            TransactionCategory.ADMINISTRATION to listOf(
                "BCA", "BANK CENTRAL ASIA", "BNI", "BRI", "BRIMO", "MANDIRI", "LIVIN",
                "CIMB", "CIMB NIAGA", "OCTO", "PERMATA", "DANAMON", "BTN", "BSI",
                "BANK SYARIAH INDONESIA", "JAGO", "JENIUS", "BTPN", "SEABANK", "ALLO BANK",
                "ALLO", "MAYBANK", "OCBC", "PANIN", "BANK MEGA", "SINARMAS", "MUAMALAT",
                "BUKOPIN", "DBS", "DIGIBANK", "UOB", "HSBC", "NEO COMMERCE", "BLU",
                "LINE BANK", "SUPERBANK", "KROM",
                "QRIS", "GOPAY", "OVO", "DANA", "SHOPEEPAY", "LINKAJA", "SAKUKU", "ISAKU",
                "ASTRAPAY", "DOKU", "MIDTRANS", "XENDIT", "FLIP", "VIRTUAL ACCOUNT"
            )
        )
    }

    private fun MutableMap<String, TransactionCategory>.putGroups(
        vararg groups: Pair<TransactionCategory, List<String>>
    ) {
        groups.forEach { (category, keywords) ->
            keywords.forEach { put(normalizeForMatch(it).trim(), category) }
        }
    }
}

/** Uppercases, replaces every non-alphanumeric character with a space, and pads with spaces. */
internal fun normalizeForMatch(text: String): String {
    val cleaned = buildString {
        text.uppercase().forEach { append(if (it.isLetterOrDigit()) it else ' ') }
    }
    return " " + cleaned.split(' ').filter(String::isNotEmpty).joinToString(" ") + " "
}

/** True when [keyword] appears as a whole word inside a [normalizeForMatch] result. */
internal fun String.containsKeyword(keyword: String): Boolean = contains(" $keyword ")

internal fun String.containsAnyKeyword(keywords: List<String>): Boolean =
    keywords.any { containsKeyword(it) }
