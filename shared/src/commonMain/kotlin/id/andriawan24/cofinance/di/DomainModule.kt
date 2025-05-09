package id.andriawan24.cofinance.di

import id.andriawan24.cofinance.domain.usecase.FetchUserUseCase
import id.andriawan24.cofinance.domain.usecase.GetUserUseCase
import id.andriawan24.cofinance.domain.usecase.LoginIdTokenUseCase
import id.andriawan24.cofinance.domain.usecase.LogoutUseCase
import org.koin.dsl.module

val domainModule = module {
    single<FetchUserUseCase> { FetchUserUseCase(get()) }
    single<LoginIdTokenUseCase> { LoginIdTokenUseCase(get()) }
    single<GetUserUseCase> { GetUserUseCase(get()) }
    single<LogoutUseCase> { LogoutUseCase(get()) }
}