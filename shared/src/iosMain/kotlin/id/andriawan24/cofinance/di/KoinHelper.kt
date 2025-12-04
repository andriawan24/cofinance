package id.andriawan24.cofinance.di

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import id.andriawan24.cofinance.domain.usecase.authentication.LoginIdTokenUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.LogoutUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.GetUserUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.FetchUserUseCase

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

    private inline fun <reified T> get(): T {
        val instance: T by inject()
        return instance
    }
}