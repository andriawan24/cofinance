package id.andriawan.cofinance.data.datasource

import com.andriawan.cofinance.BuildKonfig

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class GarageStorageDataSource() {
    suspend fun upload(key: String, data: ByteArray): Result<Unit>
}

val STORAGE_BASE_URL = BuildKonfig.STORAGE_BASE_URL
val STORAGE_ACCESS_KEY = BuildKonfig.STORAGE_ACCESS_KEY
val STORAGE_ACCESS_SECRET = BuildKonfig.STORAGE_ACCESS_SECRET
val STORAGE_BUCKET_NAME = BuildKonfig.STORAGE_BUCKET_NAME
val STORAGE_REGION = BuildKonfig.STORAGE_REGION
