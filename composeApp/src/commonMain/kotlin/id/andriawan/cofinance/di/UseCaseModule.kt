package id.andriawan.cofinance.di

import id.andriawan.cofinance.domain.usecases.authentications.FetchUserUseCase
import id.andriawan.cofinance.domain.usecases.transactions.GetBalanceStatsUseCase
import id.andriawan.cofinance.domain.usecases.transactions.GetTransactionsGroupByMonthUseCase
import org.koin.dsl.module

val useCaseModule = module {
    single { FetchUserUseCase(get()) }
    single { GetTransactionsGroupByMonthUseCase(get()) }
    single { GetBalanceStatsUseCase(get()) }
}
