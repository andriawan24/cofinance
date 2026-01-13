package id.andriawan.cofinance.di

import id.andriawan.cofinance.domain.usecases.authentications.FetchUserUseCase
import org.koin.dsl.module

val useCaseModule = module {
    single { FetchUserUseCase(get()) }
}
