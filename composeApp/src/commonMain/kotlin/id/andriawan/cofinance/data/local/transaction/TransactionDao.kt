package id.andriawan.cofinance.data.local.transaction

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.andriawan.cofinance.data.model.entity.LocalTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun watchTransactions(): Flow<List<LocalTransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getTransactions(): List<LocalTransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransaction(id: String): LocalTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransaction(transaction: LocalTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTransactions(transactions: List<LocalTransactionEntity>)

    @Query(
        "SELECT * FROM transactions WHERE senderAccountId = :accountId " +
                "OR receiverAccountId = :accountId"
    )
    suspend fun getTransactionsForAccount(accountId: String): List<LocalTransactionEntity>

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
