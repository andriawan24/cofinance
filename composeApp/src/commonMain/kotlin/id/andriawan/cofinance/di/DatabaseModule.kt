package id.andriawan.cofinance.di

import id.andriawan.cofinance.data.local.account.AccountLocalDataSource
import id.andriawan.cofinance.data.local.account.RoomAccountLocalDataSource
import id.andriawan.cofinance.data.local.buildLocalDatabase
import id.andriawan.cofinance.data.local.merchant.MerchantCategoryLocalDataSource
import id.andriawan.cofinance.data.local.merchant.RoomMerchantCategoryLocalDataSource
import id.andriawan.cofinance.data.local.transaction.RoomTransactionLocalDataSource
import id.andriawan.cofinance.data.local.transaction.TransactionLocalDataSource
import id.andriawan.cofinance.data.ocr.parser.MerchantCategoryLearning
import id.andriawan.cofinance.data.remote.AccountRemoteDataSource
import id.andriawan.cofinance.data.remote.EncryptedAccountDataSource
import id.andriawan.cofinance.data.remote.EncryptedTransactionDataSource
import id.andriawan.cofinance.data.remote.FinanceDocumentStore
import id.andriawan.cofinance.data.remote.FirestoreFinanceDocumentStore
import id.andriawan.cofinance.data.remote.KeyMaterialGate
import id.andriawan.cofinance.data.remote.TransactionRemoteDataSource
import id.andriawan.cofinance.data.sync.FirebaseSyncCoordinator
import coil3.PlatformContext
import id.andriawan.cofinance.data.datasource.GarageStorageDataSource
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

fun databaseModule(context: PlatformContext) = module {
    single { buildLocalDatabase(context) }
    singleOf(::RoomAccountLocalDataSource) { bind<AccountLocalDataSource>() }
    singleOf(::RoomTransactionLocalDataSource) { bind<TransactionLocalDataSource>() }
    singleOf(::RoomMerchantCategoryLocalDataSource) { bind<MerchantCategoryLocalDataSource>() }
    singleOf(::FirestoreFinanceDocumentStore) { bind<FinanceDocumentStore>() }
    singleOf(::KeyMaterialGate)
    singleOf(::EncryptedAccountDataSource) { bind<AccountRemoteDataSource>() }
    singleOf(::EncryptedTransactionDataSource) { bind<TransactionRemoteDataSource>() }
    singleOf(::FirebaseSyncCoordinator)
    single { GarageStorageDataSource() }
    single { MerchantCategoryLearning(get(), get()) }
}
