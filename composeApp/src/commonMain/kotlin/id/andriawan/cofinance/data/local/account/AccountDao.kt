package id.andriawan.cofinance.data.local.account

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import id.andriawan.cofinance.data.model.entity.LocalAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY createdAt DESC")
    fun watchAccounts(): Flow<List<LocalAccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY createdAt DESC")
    suspend fun getAccounts(): List<LocalAccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccount(id: String): LocalAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: LocalAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccounts(accounts: List<LocalAccountEntity>)

    @Update
    suspend fun updateAccount(account: LocalAccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteAccount(id: String)

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()
}
