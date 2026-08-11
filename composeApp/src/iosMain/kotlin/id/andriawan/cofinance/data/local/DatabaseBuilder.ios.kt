package id.andriawan.cofinance.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import coil3.PlatformContext
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual fun databaseBuilder(context: PlatformContext): RoomDatabase.Builder<CofinanceRoomDatabase> {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )?.path ?: error("Unable to resolve application documents directory")
    return Room.databaseBuilder<CofinanceRoomDatabase>(name = "$documentDirectory/cofinance.db")
}
