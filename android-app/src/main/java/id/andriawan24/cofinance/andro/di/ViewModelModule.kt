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
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::LoginViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::ActivityViewModel)
    viewModelOf(::SplashViewModel)
    viewModelOf(::CameraViewModel)
    viewModelOf(::PreviewViewModel)
    viewModelOf(::AddNewViewModel)
    viewModelOf(::AddAccountViewModel)
    viewModelOf(::AccountViewModel)
}