package id.andriawan24.cofinance.di

import id.andriawan24.cofinance.domain.usecase.authentication.FetchUserUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.GetUserUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.LoginIdTokenUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.LogoutUseCase
import id.andriawan24.cofinance.domain.usecase.transaction.GetTransactionsUseCase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin

fun initKoin() {
    startKoin {
        modules(dataModule, domainModule)
    }
}

class KoinHelper : KoinComponent {
    fun getLoginIdTokenUseCase(): LoginIdTokenUseCase = get()
    fun getLogoutUseCase(): LogoutUseCase = get()
    fun getGetUserUseCase(): GetUserUseCase = get()
    fun getFetchUserUseCase(): FetchUserUseCase = get()

    fun getTransactionsUseCase(): GetTransactionsUseCase = get()

    private inline fun <reified T> get(): T {
        val instance: T by inject()
        return instance
    }
}