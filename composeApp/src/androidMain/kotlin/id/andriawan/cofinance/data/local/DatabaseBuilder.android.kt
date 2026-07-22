package id.andriawan.cofinance.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import coil3.PlatformContext

actual fun databaseBuilder(context: PlatformContext): RoomDatabase.Builder<CofinanceRoomDatabase> =
    Room.databaseBuilder<CofinanceRoomDatabase>(
        context = context.applicationContext,
        name = context.getDatabasePath("cofinance.db").absolutePath
    )
