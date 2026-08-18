package id.andriawan.cofinance.data.datasource

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.net.url.Url

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class GarageStorageDataSource {

    private val s3Client = S3Client {
        region = STORAGE_REGION
        endpointUrl = Url.parse(STORAGE_BASE_URL)
        forcePathStyle = true
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = STORAGE_ACCESS_KEY
            secretAccessKey = STORAGE_ACCESS_SECRET
        }
    }

    actual suspend fun upload(key: String, data: ByteArray): Result<Unit> = runCatching {
        s3Client.putObject(PutObjectRequest {
            this.bucket = STORAGE_BUCKET_NAME
            this.key = key
            this.body = ByteStream.fromBytes(data)
        })
    }
}
