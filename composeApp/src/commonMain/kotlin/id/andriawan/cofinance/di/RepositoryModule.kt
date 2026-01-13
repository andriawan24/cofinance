package id.andriawan.cofinance.di

import id.andriawan.cofinance.data.repository.AccountRepository
import id.andriawan.cofinance.data.repository.AccountRepositoryImpl
import id.andriawan.cofinance.data.repository.AuthenticationRepository
import id.andriawan.cofinance.data.repository.AuthenticationRepositoryImpl
import id.andriawan.cofinance.data.repository.TransactionRepository
import id.andriawan.cofinance.data.repository.TransactionRepositoryImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val repositoryModule = module {
    singleOf(::AuthenticationRepositoryImpl) { bind<AuthenticationRepository>() }
    singleOf(::AccountRepositoryImpl) { bind<AccountRepository>() }
    singleOf(::TransactionRepositoryImpl) { bind<TransactionRepository>() }
}
