package id.andriawan.cofinance.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import id.andriawan.cofinance.data.local.account.AccountDao
import id.andriawan.cofinance.data.local.transaction.TransactionDao
import id.andriawan.cofinance.data.model.entity.LocalAccountEntity
import id.andriawan.cofinance.data.model.entity.LocalTransactionEntity

@Database(
    entities = [LocalAccountEntity::class, LocalTransactionEntity::class],
    version = 1,
    exportSchema = true
)
@ConstructedBy(CofinanceRoomDatabaseConstructor::class)
abstract class CofinanceRoomDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
}

expect object CofinanceRoomDatabaseConstructor : RoomDatabaseConstructor<CofinanceRoomDatabase> {
    override fun initialize(): CofinanceRoomDatabase
}
