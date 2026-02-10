package id.andriawan.cofinance.utils

import coil3.PlatformContext

actual fun deleteFile(fileUri: String) {
    // No-op: web has no local file system to clean up
}

actual fun readFromFile(context: PlatformContext, fileUri: String): ByteArray? {
    return null
//    return try {
//        val xhr = XMLHttpRequest()
//        xhr.open("GET", fileUri, false)
//        xhr.overrideMimeType("text/plain; charset=x-user-defined")
//        xhr.send()
//
//        if (xhr.status == 200.toShort() || xhr.status == 0.toShort()) {
//            val text = xhr.responseText
//            ByteArray(text.length) { text[it].code.toByte() }
//        } else {
//            null
//        }
//    } catch (_: Exception) {
//        null
//    }
}
