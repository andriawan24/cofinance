package id.andriawan.cofinance.data.datasource

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.dataWithBytes
import platform.Foundation.setHTTPMethod
import platform.Foundation.setValue
import platform.Foundation.uploadTaskWithRequest
import kotlin.coroutines.resume

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class GarageStorageDataSource {
    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun upload(key: String, data: ByteArray): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            val url = NSURL.URLWithString("$STORAGE_BASE_URL/$STORAGE_BUCKET_NAME/$key")!!

            val request = NSMutableURLRequest.requestWithURL(url).apply {
                setHTTPMethod("PUT")
                setValue("application/octet-stream", forHTTPHeaderField = "Content-Type")
            }

            val nsData = data.usePinned { pinned ->
                NSData.dataWithBytes(pinned.addressOf(0), data.size.toULong())
            }

            val task = NSURLSession.sharedSession.uploadTaskWithRequest(
                request = request,
                fromData = nsData
            ) { _, _, error ->
                if (error != null) {
                    continuation.resume(Result.failure(Exception(error.localizedDescription)))
                } else {
                    continuation.resume(Result.success(Unit))
                }
            }

            task.resume()
        }
}
