package id.andriawan.cofinance.di

import id.andriawan.cofinance.domain.usecases.accounts.GetAccountsUseCase
import id.andriawan.cofinance.domain.usecases.authentications.FetchUserUseCase
import id.andriawan.cofinance.domain.usecases.authentications.GetUserUseCase
import id.andriawan.cofinance.domain.usecases.authentications.LogoutUseCase
import id.andriawan.cofinance.domain.usecases.transactions.GetBalanceStatsUseCase
import id.andriawan.cofinance.domain.usecases.transactions.GetTransactionsGroupByMonthUseCase
import id.andriawan.cofinance.domain.usecases.transactions.GetTransactionsUseCase
import org.koin.dsl.module

val useCaseModule = module {
    single { FetchUserUseCase(get()) }
    single { GetTransactionsGroupByMonthUseCase(get()) }
    single { GetBalanceStatsUseCase(get()) }
    single { GetTransactionsUseCase(get()) }
    single { GetAccountsUseCase(get()) }
    single { GetUserUseCase(get()) }
    single { LogoutUseCase(get()) }
}
