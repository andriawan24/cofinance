package id.andriawan.cofinance.di

import id.andriawan.cofinance.data.local.CofinanceDatabase
import id.andriawan.cofinance.data.local.FirestoreCofinanceDatabase
import id.andriawan.cofinance.data.local.RemoteFinanceDataSource
import id.andriawan.cofinance.data.local.RoomCofinanceDatabase
import id.andriawan.cofinance.data.local.buildLocalDatabase
import id.andriawan.cofinance.data.sync.FinanceSyncCoordinator
import coil3.PlatformContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun databaseModule(context: PlatformContext) = module {
    single { buildLocalDatabase(context) }
    singleOf(::RoomCofinanceDatabase) { bind<CofinanceDatabase>() }
    singleOf(::FirestoreCofinanceDatabase) { bind<RemoteFinanceDataSource>() }
    singleOf(::FinanceSyncCoordinator)
}
