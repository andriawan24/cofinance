package id.andriawan24.cofinance.di

import id.andriawan24.cofinance.domain.usecase.accounts.AddAccountUseCase
import id.andriawan24.cofinance.domain.usecase.accounts.GetAccountsUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.FetchUserUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.GetUserUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.LoginIdTokenUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.LogoutUseCase
import id.andriawan24.cofinance.domain.usecase.transaction.CreateTransactionUseCase
import id.andriawan24.cofinance.domain.usecase.transaction.GetTransactionsUseCase
import id.andriawan24.cofinance.domain.usecase.transaction.ScanReceiptUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val domainModule = module {
    // MARK: User management
    singleOf(::LoginIdTokenUseCase)
    singleOf(::FetchUserUseCase)
    singleOf(::GetUserUseCase)
    singleOf(::LogoutUseCase)

    // MARK: Transactions
    singleOf(::ScanReceiptUseCase)
    singleOf(::GetTransactionsUseCase)
    singleOf(::CreateTransactionUseCase)

    // MARK: Accounts
    singleOf(::GetAccountsUseCase)
    singleOf(::AddAccountUseCase)
}