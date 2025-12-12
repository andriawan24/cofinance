package id.andriawan24.cofinance.utils.ext

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun String.toInstant(): Instant {
    if (this.isBlank()) {
        return Clock.System.now()
    }
    val instant = Instant.parse(this)
    return instant
}