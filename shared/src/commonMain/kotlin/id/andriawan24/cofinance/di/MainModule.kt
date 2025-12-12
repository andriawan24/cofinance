package id.andriawan24.cofinance.di

import id.andriawan24.cofinance.data.datasource.GeminiDataSource
import id.andriawan24.cofinance.data.datasource.SupabaseDataSource
import id.andriawan24.cofinance.data.repository.AccountRepository
import id.andriawan24.cofinance.data.repository.AccountRepositoryImpl
import id.andriawan24.cofinance.data.repository.AuthenticationRepository
import id.andriawan24.cofinance.data.repository.AuthenticationRepositoryImpl
import id.andriawan24.cofinance.data.repository.TransactionRepository
import id.andriawan24.cofinance.data.repository.TransactionRepositoryImpl
import id.andriawan24.cofinance.domain.usecase.accounts.AddAccountUseCase
import id.andriawan24.cofinance.domain.usecase.accounts.GetAccountsUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.FetchUserUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.GetUserUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.LoginIdTokenUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.LogoutUseCase
import id.andriawan24.cofinance.domain.usecase.transaction.CreateTransactionUseCase
import id.andriawan24.cofinance.domain.usecase.transaction.GetTransactionsGroupByMonthUseCase
import id.andriawan24.cofinance.domain.usecase.transaction.GetTransactionsUseCase
import id.andriawan24.cofinance.domain.usecase.transaction.ScanReceiptUseCase
import id.andriawan24.cofinance.utils.GeminiHelper
import id.andriawan24.cofinance.utils.SupabaseClientHelper
import io.github.jan.supabase.SupabaseClient
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Main Koin module using constructor DSL for compile-time safety.
 *
 * This approach provides compile-time verification of constructor parameters
 * and ensures type safety across the dependency graph.
 */
val mainModule = module {
    // Data Layer - Factories
    single<SupabaseClient> { SupabaseClientHelper.createClient() }
    single<Json> {
        Json {
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
    single<GeminiDataSource> { GeminiDataSource(GeminiHelper.createModel(), get()) }

    // Data Layer - Data Sources
    singleOf(::SupabaseDataSource)

    // Data Layer - Repositories
    singleOf(::AccountRepositoryImpl) { bind<AccountRepository>() }
    singleOf(::AuthenticationRepositoryImpl) { bind<AuthenticationRepository>() }
    singleOf(::TransactionRepositoryImpl) { bind<TransactionRepository>() }

    // Domain Layer - Use Cases
    singleOf(::AddAccountUseCase)
    singleOf(::GetAccountsUseCase)
    singleOf(::FetchUserUseCase)
    singleOf(::GetUserUseCase)
    singleOf(::LoginIdTokenUseCase)
    singleOf(::LogoutUseCase)
    singleOf(::CreateTransactionUseCase)
    singleOf(::GetTransactionsGroupByMonthUseCase)
    singleOf(::GetTransactionsUseCase)
    singleOf(::ScanReceiptUseCase)
}