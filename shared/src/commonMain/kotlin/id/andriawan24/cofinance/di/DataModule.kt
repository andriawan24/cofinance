package id.andriawan24.cofinance.di

import id.andriawan24.cofinance.data.datasource.GeminiDataSource
import id.andriawan24.cofinance.data.datasource.SupabaseDataSource
import id.andriawan24.cofinance.data.repository.AccountRepository
import id.andriawan24.cofinance.data.repository.AccountRepositoryImpl
import id.andriawan24.cofinance.data.repository.AuthenticationRepository
import id.andriawan24.cofinance.data.repository.AuthenticationRepositoryImpl
import id.andriawan24.cofinance.data.repository.TransactionRepository
import id.andriawan24.cofinance.data.repository.TransactionRepositoryImpl
import id.andriawan24.cofinance.utils.GeminiHelper
import id.andriawan24.cofinance.utils.SupabaseClientHelper
import io.github.jan.supabase.SupabaseClient
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val dataModule = module {
    single<SupabaseClient> { SupabaseClientHelper.createClient() }
    single<SupabaseDataSource> { SupabaseDataSource(get()) }
    single<GeminiDataSource> {
        GeminiDataSource(
            model = GeminiHelper.createModel(),
            json = Json {
                isLenient = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        )
    }

    single<AuthenticationRepository> { AuthenticationRepositoryImpl(get()) }
    single<TransactionRepository> { TransactionRepositoryImpl(get()) }
    single<AccountRepository> { AccountRepositoryImpl(get()) }
}