package id.andriawan.cofinance.di

import id.andriawan.cofinance.auth.GoogleAuthManager
import id.andriawan.cofinance.data.datasource.FirebaseDataSource
import id.andriawan.cofinance.data.datasource.GeminiDataSource
import id.andriawan.cofinance.data.datasource.ReceiptScanner
import id.andriawan.cofinance.data.session.FirebaseSessionPolicy
import id.andriawan.cofinance.data.session.SessionPolicy
import id.andriawan.cofinance.utils.GeminiHelper
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.storage.storage
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.core.module.dsl.bind

val networkModule = module {
    single { Firebase.auth }
    single { Firebase.firestore }
    single { Firebase.storage }
    singleOf(::FirebaseSessionPolicy) { bind<SessionPolicy>() }
    single<Json> {
        Json {
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
    single<ReceiptScanner> { GeminiDataSource(GeminiHelper.createModel(), get()) }
    singleOf(::FirebaseDataSource)
    singleOf(::GoogleAuthManager)
}
