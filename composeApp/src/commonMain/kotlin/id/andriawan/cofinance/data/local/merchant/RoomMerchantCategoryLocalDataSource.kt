package id.andriawan.cofinance.data.local.merchant

import id.andriawan.cofinance.data.local.CofinanceRoomDatabase
import id.andriawan.cofinance.data.model.entity.LocalMerchantCategoryEntity
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class RoomMerchantCategoryLocalDataSource(
    roomDatabase: CofinanceRoomDatabase
) : MerchantCategoryLocalDataSource {
    private val dao = roomDatabase.merchantCategoryDao()

    override suspend fun getAssociations(): Map<String, String> =
        dao.getAssociations().associate { it.merchantKey to it.category }

    override suspend fun getAssociation(merchantKey: String): String? =
        dao.getAssociation(merchantKey)?.category

    override suspend fun recordAssociation(merchantKey: String, category: String) {
        if (merchantKey.isBlank() || category.isBlank()) return
        dao.upsertAssociation(
            LocalMerchantCategoryEntity(merchantKey, category, Clock.System.now().toString())
        )
    }

    override suspend fun clearAssociations() {
        dao.deleteAllAssociations()
    }
}
