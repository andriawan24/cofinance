package id.andriawan.cofinance

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform