package id.andriawan.cofinance.di

import id.andriawan.cofinance.data.local.CofinanceDatabase
import id.andriawan.cofinance.data.local.FirestoreCofinanceDatabase
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val databaseModule = module {
    singleOf(::FirestoreCofinanceDatabase) { bind<CofinanceDatabase>() }
}
