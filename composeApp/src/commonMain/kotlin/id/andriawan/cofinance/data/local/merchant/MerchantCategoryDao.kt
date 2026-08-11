package id.andriawan.cofinance.data.local.merchant

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.andriawan.cofinance.data.model.entity.LocalMerchantCategoryEntity

@Dao
interface MerchantCategoryDao {
    @Query("SELECT * FROM merchant_categories")
    suspend fun getAssociations(): List<LocalMerchantCategoryEntity>

    @Query("SELECT * FROM merchant_categories WHERE merchantKey = :merchantKey")
    suspend fun getAssociation(merchantKey: String): LocalMerchantCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssociation(association: LocalMerchantCategoryEntity)

    @Query("DELETE FROM merchant_categories")
    suspend fun deleteAllAssociations()
}
