package id.andriawan24.cofinance.di

import id.andriawan24.cofinance.domain.usecase.SignInIdTokenUseCase
import org.koin.dsl.module

val domainModule = module {
    single<SignInIdTokenUseCase> { SignInIdTokenUseCase(get()) }
}