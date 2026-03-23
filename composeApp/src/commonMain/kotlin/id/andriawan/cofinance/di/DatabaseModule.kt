package id.andriawan.cofinance.di

import id.andriawan.cofinance.data.local.CofinanceDatabase
import org.koin.dsl.module

fun databaseModule(database: CofinanceDatabase) = module {
    single<CofinanceDatabase> { database }
}
