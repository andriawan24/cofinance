package id.andriawan24.cofinance.di

import id.andriawan24.cofinance.domain.usecase.accounts.AddAccountUseCase
import id.andriawan24.cofinance.domain.usecase.accounts.GetAccountsUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.FetchUserUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.GetUserUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.LoginIdTokenUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.LogoutUseCase
import id.andriawan24.cofinance.domain.usecase.transaction.ScanReceiptUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val domainModule = module {
    singleOf(::FetchUserUseCase)
    singleOf(::LoginIdTokenUseCase)
    singleOf(::GetUserUseCase)
    singleOf(::LogoutUseCase)
    singleOf(::ScanReceiptUseCase)
    singleOf(::GetAccountsUseCase)
    singleOf(::AddAccountUseCase)
}