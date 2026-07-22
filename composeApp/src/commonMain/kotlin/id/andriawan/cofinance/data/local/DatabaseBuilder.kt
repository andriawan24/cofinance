package id.andriawan.cofinance.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import coil3.PlatformContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

expect fun databaseBuilder(context: PlatformContext): RoomDatabase.Builder<CofinanceRoomDatabase>

fun buildLocalDatabase(context: PlatformContext): CofinanceRoomDatabase =
    databaseBuilder(context)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
