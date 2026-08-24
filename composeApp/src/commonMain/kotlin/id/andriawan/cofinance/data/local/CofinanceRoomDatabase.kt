package id.andriawan.cofinance.data.local

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import id.andriawan.cofinance.data.local.account.AccountDao
import id.andriawan.cofinance.data.local.merchant.MerchantCategoryDao
import id.andriawan.cofinance.data.local.transaction.TransactionDao
import id.andriawan.cofinance.data.model.entity.LocalAccountEntity
import id.andriawan.cofinance.data.model.entity.LocalMerchantCategoryEntity
import id.andriawan.cofinance.data.model.entity.LocalTransactionEntity

@Database(
    entities = [
        LocalAccountEntity::class,
        LocalTransactionEntity::class,
        LocalMerchantCategoryEntity::class
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
@ConstructedBy(CofinanceRoomDatabaseConstructor::class)
abstract class CofinanceRoomDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun merchantCategoryDao(): MerchantCategoryDao
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object CofinanceRoomDatabaseConstructor : RoomDatabaseConstructor<CofinanceRoomDatabase> {
    override fun initialize(): CofinanceRoomDatabase
}
