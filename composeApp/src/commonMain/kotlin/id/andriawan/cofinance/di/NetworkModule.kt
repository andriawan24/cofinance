package id.andriawan.cofinance.di

import id.andriawan.cofinance.auth.GoogleAuthManager
import id.andriawan.cofinance.data.datasource.FirebaseDataSource
import id.andriawan.cofinance.data.datasource.OnDeviceReceiptScanner
import id.andriawan.cofinance.data.datasource.ReceiptScanner
import id.andriawan.cofinance.data.crypto.DeviceKeyVault
import id.andriawan.cofinance.data.crypto.DeviceKeyWrapper
import id.andriawan.cofinance.data.crypto.RecordCipher
import id.andriawan.cofinance.data.crypto.RecoveryPhraseKeyWrapper
import id.andriawan.cofinance.data.crypto.createDeviceKeyVault
import id.andriawan.cofinance.data.keyring.EncryptionSession
import id.andriawan.cofinance.data.keyring.InMemoryEncryptionSession
import id.andriawan.cofinance.data.ocr.OcrEngine
import id.andriawan.cofinance.data.ocr.createOcrEngine
import id.andriawan.cofinance.data.ocr.parser.ReceiptParser
import id.andriawan.cofinance.data.session.FirebaseSessionPolicy
import id.andriawan.cofinance.data.session.SessionPolicy
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.storage.storage
import kotlin.time.ExperimentalTime
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.core.module.dsl.bind

@OptIn(ExperimentalTime::class)
val networkModule = module {
    single { Firebase.auth }
    single { Firebase.firestore }
    single { Firebase.storage }
    singleOf(::FirebaseSessionPolicy) { bind<SessionPolicy>() }
    // One session per process: the unwrapped data key exists only inside it, and the setup, unlock,
    // and synchronization paths all have to be looking at the same lock state.
    singleOf(::InMemoryEncryptionSession) { bind<EncryptionSession>() }
    single { RecordCipher() }
    // The device key vault is created lazily: it reaches platform key storage on first use, which
    // must not happen while the graph is being built.
    single<DeviceKeyVault> { createDeviceKeyVault() }
    single { DeviceKeyWrapper(get()) }
    single { RecoveryPhraseKeyWrapper() }
    // The durable local key material store lives in securityModule, next to the lock that reads
    // and erases it.
    single<Json> {
        Json {
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
    single<OcrEngine> { createOcrEngine() }
    single { ReceiptParser() }
    single<ReceiptScanner> { OnDeviceReceiptScanner(get(), get()) }
    singleOf(::FirebaseDataSource)
    singleOf(::GoogleAuthManager)
}
