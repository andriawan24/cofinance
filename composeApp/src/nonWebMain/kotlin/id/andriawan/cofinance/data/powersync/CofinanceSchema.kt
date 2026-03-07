package id.andriawan.cofinance.data.powersync

import com.powersync.db.schema.Column
import com.powersync.db.schema.Index
import com.powersync.db.schema.IndexedColumn
import com.powersync.db.schema.Schema
import com.powersync.db.schema.Table

val CofinanceSchema = Schema(
    listOf(
        Table(
            name = "accounts",
            columns = listOf(
                Column.text("name"),
                Column.text("group"),
                Column.integer("balance"),
                Column.text("users_id"),
                Column.text("created_at")
            ),
            indexes = listOf(
                Index("idx_accounts_user", listOf(IndexedColumn.ascending("users_id")))
            )
        ),
        Table(
            name = "transactions",
            columns = listOf(
                Column.integer("amount"),
                Column.text("category"),
                Column.text("date"),
                Column.integer("fee"),
                Column.text("notes"),
                Column.text("accounts_id"),
                Column.text("receiver_accounts_id"),
                Column.text("type"),
                Column.text("users_id"),
                Column.text("created_at"),
                Column.text("updated_at")
            ),
            indexes = listOf(
                Index("idx_tx_date", listOf(IndexedColumn.descending("date"))),
                Index("idx_tx_user", listOf(IndexedColumn.ascending("users_id"))),
                Index("idx_tx_account", listOf(IndexedColumn.ascending("accounts_id")))
            )
        )
    )
)
