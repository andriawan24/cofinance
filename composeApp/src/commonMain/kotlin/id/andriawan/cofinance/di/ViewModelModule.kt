package id.andriawan.cofinance.di

import id.andriawan.cofinance.pages.account.AccountViewModel
import id.andriawan.cofinance.pages.activity.ActivityViewModel
import id.andriawan.cofinance.pages.addnew.AddAccountViewModel
import id.andriawan.cofinance.pages.addnew.AddNewViewModel
import id.andriawan.cofinance.pages.camera.CameraViewModel
import id.andriawan.cofinance.pages.preview.PreviewViewModel
import id.andriawan.cofinance.pages.profile.ProfileViewModel
import id.andriawan.cofinance.pages.splash.SplashViewModel
import id.andriawan.cofinance.pages.stats.StatsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::SplashViewModel)
    viewModelOf(::ActivityViewModel)
    viewModelOf(::StatsViewModel)
    viewModelOf(::AccountViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::AddNewViewModel)
    viewModelOf(::CameraViewModel)
    viewModelOf(::PreviewViewModel)
    viewModelOf(::AddAccountViewModel)
}
