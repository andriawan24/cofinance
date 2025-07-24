package id.andriawan24.cofinance.andro

import android.app.Application
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

class MainApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setupNapier()
    }

    private fun setupNapier() {
        Napier.base(DebugAntilog())
    }
}