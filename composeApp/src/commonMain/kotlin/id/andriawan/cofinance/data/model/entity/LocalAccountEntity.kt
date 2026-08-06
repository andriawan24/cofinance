package id.andriawan.cofinance.data.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class LocalAccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val group: String,
    val balance: Long,
    val accountType: String,
    val createdAt: String
)
