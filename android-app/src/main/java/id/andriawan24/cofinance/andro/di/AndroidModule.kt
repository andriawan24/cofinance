package id.andriawan24.cofinance.andro.di

import id.andriawan24.cofinance.andro.ui.presentation.account.AccountViewModel
import id.andriawan24.cofinance.andro.ui.presentation.activity.ActivityViewModel
import id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels.AddAccountViewModel
import id.andriawan24.cofinance.andro.ui.presentation.addnew.viewmodels.AddNewViewModel
import id.andriawan24.cofinance.andro.ui.presentation.camera.CameraViewModel
import id.andriawan24.cofinance.andro.ui.presentation.login.LoginViewModel
import id.andriawan24.cofinance.andro.ui.presentation.preview.PreviewViewModel
import id.andriawan24.cofinance.andro.ui.presentation.profile.ProfileViewModel
import id.andriawan24.cofinance.andro.ui.presentation.splashscreen.SplashViewModel
import id.andriawan24.cofinance.andro.ui.presentation.stats.StatsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Android-specific Koin module using constructor DSL for compile-time safety.
 *
 * This approach provides compile-time verification of ViewModel constructor
 * parameters and ensures type safety across the dependency graph.
 */
val androidModule = module {
    viewModelOf(::LoginViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::ActivityViewModel)
    viewModelOf(::SplashViewModel)
    viewModelOf(::CameraViewModel)
    viewModelOf(::PreviewViewModel)
    viewModelOf(::AddNewViewModel)
    viewModelOf(::AddAccountViewModel)
    viewModelOf(::AccountViewModel)
    viewModelOf(::StatsViewModel)
}
