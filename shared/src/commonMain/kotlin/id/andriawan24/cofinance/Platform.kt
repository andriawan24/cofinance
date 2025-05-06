package id.andriawan24.cofinance

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform