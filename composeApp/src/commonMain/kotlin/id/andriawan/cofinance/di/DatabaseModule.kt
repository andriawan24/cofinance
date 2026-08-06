package id.andriawan.cofinance.di

import id.andriawan.cofinance.data.local.account.AccountLocalDataSource
import id.andriawan.cofinance.data.local.account.RoomAccountLocalDataSource
import id.andriawan.cofinance.data.local.buildLocalDatabase
import id.andriawan.cofinance.data.local.transaction.RoomTransactionLocalDataSource
import id.andriawan.cofinance.data.local.transaction.TransactionLocalDataSource
import id.andriawan.cofinance.data.remote.AccountRemoteDataSource
import id.andriawan.cofinance.data.remote.FirestoreAccountDataSource
import id.andriawan.cofinance.data.remote.FirestoreTransactionDataSource
import id.andriawan.cofinance.data.remote.TransactionRemoteDataSource
import id.andriawan.cofinance.data.sync.FinanceSyncCoordinator
import coil3.PlatformContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun databaseModule(context: PlatformContext) = module {
    single { buildLocalDatabase(context) }
    singleOf(::RoomAccountLocalDataSource) { bind<AccountLocalDataSource>() }
    singleOf(::RoomTransactionLocalDataSource) { bind<TransactionLocalDataSource>() }
    singleOf(::FirestoreAccountDataSource) { bind<AccountRemoteDataSource>() }
    singleOf(::FirestoreTransactionDataSource) { bind<TransactionRemoteDataSource>() }
    singleOf(::FinanceSyncCoordinator)
}
