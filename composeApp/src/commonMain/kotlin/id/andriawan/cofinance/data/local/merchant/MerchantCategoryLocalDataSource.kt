package id.andriawan.cofinance.data.local.merchant

/**
 * Durable local persistence for merchant-to-category associations learned from user
 * corrections.
 *
 * Deliberately kept outside the aggregates the sync coordinator mirrors: this contract
 * has no remote counterpart, so an association can only ever be read back on the device
 * that recorded it.
 */
interface MerchantCategoryLocalDataSource {
    /** Every learned association, keyed by merchant key. */
    suspend fun getAssociations(): Map<String, String>

    suspend fun getAssociation(merchantKey: String): String?

    suspend fun recordAssociation(merchantKey: String, category: String)

    /** Wipes all learned associations, e.g. on logout. */
    suspend fun clearAssociations()
}
