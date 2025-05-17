package id.andriawan24.cofinance.di

import id.andriawan24.cofinance.domain.usecase.FetchUserUseCase
import id.andriawan24.cofinance.domain.usecase.GetUserUseCase
import id.andriawan24.cofinance.domain.usecase.LoginIdTokenUseCase
import id.andriawan24.cofinance.domain.usecase.LogoutUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val domainModule = module {
    singleOf(::FetchUserUseCase)
    singleOf(::LoginIdTokenUseCase)
    singleOf(::GetUserUseCase)
    singleOf(::LogoutUseCase)
}