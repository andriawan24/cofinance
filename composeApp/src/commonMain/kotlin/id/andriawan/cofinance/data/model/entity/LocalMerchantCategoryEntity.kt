package id.andriawan.cofinance.data.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A category the user picked for a merchant, learned from a correction on a scanned draft.
 *
 * Device-local only: no remote data source reads this table and the sync coordinator
 * never sees it, so corrections never leave the device.
 */
@Entity(tableName = "merchant_categories")
data class LocalMerchantCategoryEntity(
    @PrimaryKey val merchantKey: String,
    val category: String,
    val updatedAt: String
)
