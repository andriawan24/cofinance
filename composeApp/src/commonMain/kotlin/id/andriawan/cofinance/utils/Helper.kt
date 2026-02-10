package id.andriawan.cofinance.utils

import coil3.PlatformContext
import coil3.Uri

fun emptyString() = ""

expect fun readFromFile(context: PlatformContext, fileUri: String): ByteArray?

expect fun deleteFile(fileUri: String)
