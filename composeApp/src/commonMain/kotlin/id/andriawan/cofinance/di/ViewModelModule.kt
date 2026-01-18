package id.andriawan.cofinance.di

import id.andriawan.cofinance.pages.activity.ActivityViewModel
import id.andriawan.cofinance.pages.splash.SplashViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::SplashViewModel)
    viewModelOf(::ActivityViewModel)
}
