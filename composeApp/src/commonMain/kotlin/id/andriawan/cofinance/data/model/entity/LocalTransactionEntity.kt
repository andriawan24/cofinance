package id.andriawan.cofinance.data.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class LocalTransactionEntity(
    @PrimaryKey val id: String,
    val amount: Long,
    val category: String,
    val date: String,
    val fee: Long,
    val notes: String,
    val senderAccountId: String,
    val receiverAccountId: String?,
    val type: String,
    val createdAt: String,
    val updatedAt: String
)
