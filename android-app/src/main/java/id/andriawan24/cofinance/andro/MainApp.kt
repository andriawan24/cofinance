package id.andriawan24.cofinance.andro

import android.app.Application
import id.andriawan24.cofinance.andro.di.viewModelModule
import id.andriawan24.cofinance.di.dataModule
import id.andriawan24.cofinance.di.domainModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class MainApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MainApp)
            modules(dataModule, domainModule, viewModelModule)
        }
    }
}