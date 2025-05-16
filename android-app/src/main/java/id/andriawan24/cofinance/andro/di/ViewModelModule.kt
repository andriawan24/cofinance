package id.andriawan24.cofinance.andro.di

import id.andriawan24.cofinance.andro.ui.presentation.login.LoginViewModel
import id.andriawan24.cofinance.andro.ui.presentation.profile.ProfileViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::LoginViewModel)
    viewModelOf(::ProfileViewModel)
}