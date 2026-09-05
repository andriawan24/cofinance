package id.andriawan.cofinance.data.lock

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

fun interface LockClock {
    fun nowMillis(): Long
}

@OptIn(ExperimentalTime::class)
val systemLockClock = LockClock { Clock.System.now().toEpochMilliseconds() }
