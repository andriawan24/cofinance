package id.andriawan.cofinance.di

import id.andriawan.cofinance.auth.GoogleAuthManager
import id.andriawan.cofinance.data.datasource.ReceiptScannerService
import id.andriawan.cofinance.data.datasource.SupabaseDataSource
import id.andriawan.cofinance.data.datasource.createReceiptScanner
import id.andriawan.cofinance.utils.SupabaseHelper
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val networkModule = module {
    single { SupabaseHelper.createClient() }
    single<Json> {
        Json {
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
    single<ReceiptScannerService> { createReceiptScanner() }
    singleOf(::SupabaseDataSource)
    singleOf(::GoogleAuthManager)
}
