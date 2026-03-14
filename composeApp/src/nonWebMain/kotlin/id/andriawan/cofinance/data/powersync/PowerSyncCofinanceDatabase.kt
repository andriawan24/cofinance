package id.andriawan.cofinance.data.powersync

import com.diamondedge.logging.logging
import com.powersync.PowerSyncDatabase
import com.powersync.db.getString
import com.powersync.db.getStringOptional
import com.powersync.db.getLong
import com.powersync.db.getLongOptional
import id.andriawan.cofinance.data.local.CofinanceDatabase
import id.andriawan.cofinance.data.model.response.AccountResponse
import id.andriawan.cofinance.data.model.response.TransactionResponse
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class PowerSyncCofinanceDatabase(
    private val database: PowerSyncDatabase
) : CofinanceDatabase {

    private var connector: CofinanceConnector? = null

    // region Account reads

    override fun watchAccounts(userId: String): Flow<List<AccountResponse>> {
        val log = logging("PowerSyncDB")
        log.info { "watchAccounts called with userId='$userId'" }

        return database.watch(
            sql = "SELECT * FROM accounts WHERE users_id = ? ORDER BY created_at DESC",
            parameters = listOf(userId)
        ) { cursor ->
            AccountResponse(
                id = cursor.getString("id"),
                name = cursor.getStringOptional("name"),
                group = cursor.getStringOptional("group"),
                balance = cursor.getLongOptional("balance"),
                createdAt = cursor.getStringOptional("created_at")
            )
        }.map { accounts ->
            log.info { "watchAccounts emitted ${accounts.size} accounts" }
            accounts
        }
    }

    override suspend fun getAccounts(userId: String): List<AccountResponse> {
        return database.getAll(
            sql = "SELECT * FROM accounts WHERE users_id = ? ORDER BY created_at DESC",
            parameters = listOf(userId)
        ) { cursor ->
            AccountResponse(
                id = cursor.getString("id"),
                name = cursor.getStringOptional("name"),
                group = cursor.getStringOptional("group"),
                balance = cursor.getLongOptional("balance"),
                createdAt = cursor.getStringOptional("created_at")
            )
        }
    }

    // endregion

    // region Account writes

    override suspend fun insertAccount(
        id: String,
        name: String,
        group: String,
        balance: Long,
        userId: String
    ) {
        logging("PowerSyncDB").info { "insertAccount: id=$id, name=$name, group=$group, userId=$userId" }
        database.execute(
            sql = """
                INSERT INTO accounts (id, name, "group", balance, users_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            parameters = listOf(id, name, group, balance, userId, Clock.System.now().toString())
        )
        logging("PowerSyncDB").info { "insertAccount completed successfully" }
    }

    // endregion

    // region Transaction reads

    override fun watchTransactions(
        userId: String,
        month: Int?,
        year: Int?,
        isDraft: Boolean,
        transactionId: String?
    ): Flow<List<TransactionResponse>> {
        val (sql, params) = buildTransactionQuery(userId, month, year, isDraft, transactionId)
        return database.watch(sql = sql, parameters = params) { cursor ->
            mapTransactionRow(cursor)
        }
    }

    override suspend fun getTransactions(
        userId: String,
        month: Int?,
        year: Int?,
        isDraft: Boolean,
        transactionId: String?
    ): List<TransactionResponse> {
        val (sql, params) = buildTransactionQuery(userId, month, year, isDraft, transactionId)
        return database.getAll(sql = sql, parameters = params) { cursor ->
            mapTransactionRow(cursor)
        }
    }

    private fun buildTransactionQuery(
        userId: String,
        month: Int?,
        year: Int?,
        isDraft: Boolean,
        transactionId: String?
    ): Pair<String, List<Any?>> {
        val conditions = mutableListOf<String>()
        val params = mutableListOf<Any?>()

        conditions.add("t.users_id = ?")
        params.add(userId)

        if (month != null && year != null) {
            // Filter by month/year using date string comparison
            val startDate = "%04d-%02d-01".format(year, month)
            val endMonth = if (month == 12) 1 else month + 1
            val endYear = if (month == 12) year + 1 else year
            val endDate = "%04d-%02d-01".format(endYear, endMonth)
            conditions.add("t.date >= ?")
            params.add(startDate)
            conditions.add("t.date < ?")
            params.add(endDate)
        }

        if (transactionId != null) {
            conditions.add("t.id = ?")
            params.add(transactionId)
        }

        if (isDraft) {
            conditions.add("t.type = 'DRAFT'")
        } else {
            conditions.add("t.type != 'DRAFT'")
        }

        val whereClause = conditions.joinToString(" AND ")
        val limitClause = if (transactionId != null) "LIMIT 1" else ""

        val sql = """
            SELECT t.*,
                sa.name as sender_name, sa."group" as sender_group, sa.balance as sender_balance, sa.created_at as sender_created_at,
                ra.name as receiver_name, ra."group" as receiver_group, ra.balance as receiver_balance, ra.created_at as receiver_created_at
            FROM transactions t
            LEFT JOIN accounts sa ON t.accounts_id = sa.id
            LEFT JOIN accounts ra ON t.receiver_accounts_id = ra.id
            WHERE $whereClause
            ORDER BY t.date DESC
            $limitClause
        """.trimIndent()

        return sql to params
    }

    private fun mapTransactionRow(cursor: com.powersync.db.SqlCursor): TransactionResponse {
        return TransactionResponse(
            id = cursor.getStringOptional("id"),
            amount = cursor.getLongOptional("amount"),
            category = cursor.getStringOptional("category"),
            date = cursor.getStringOptional("date"),
            fee = cursor.getLongOptional("fee"),
            notes = cursor.getStringOptional("notes"),
            senderAccountId = cursor.getStringOptional("accounts_id"),
            receiverAccountId = cursor.getStringOptional("receiver_accounts_id"),
            createdAt = cursor.getStringOptional("created_at"),
            updatedAt = cursor.getStringOptional("updated_at"),
            type = cursor.getStringOptional("type"),
            sender = AccountResponse(
                id = cursor.getStringOptional("accounts_id"),
                name = cursor.getStringOptional("sender_name"),
                group = cursor.getStringOptional("sender_group"),
                balance = cursor.getLongOptional("sender_balance"),
                createdAt = cursor.getStringOptional("sender_created_at")
            ),
            receiver = AccountResponse(
                id = cursor.getStringOptional("receiver_accounts_id"),
                name = cursor.getStringOptional("receiver_name"),
                group = cursor.getStringOptional("receiver_group"),
                balance = cursor.getLongOptional("receiver_balance"),
                createdAt = cursor.getStringOptional("receiver_created_at")
            )
        )
    }

    // endregion

    // region Transaction writes

    override suspend fun insertTransaction(
        id: String,
        amount: Long,
        category: String,
        date: String,
        fee: Long,
        notes: String,
        accountsId: String,
        receiverAccountsId: String?,
        type: String,
        userId: String
    ) {
        val now = Clock.System.now().toString()
        database.execute(
            sql = """
                INSERT INTO transactions (id, amount, category, date, fee, notes, accounts_id, receiver_accounts_id, type, users_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            parameters = listOf(
                id, amount, category, date, fee, notes,
                accountsId, receiverAccountsId, type, userId, now, now
            )
        )
    }

    // endregion

    // region Sync lifecycle

    override suspend fun connectSync(supabaseClient: SupabaseClient, powerSyncUrl: String) {
        val conn = CofinanceConnector(supabaseClient, powerSyncUrl)
        connector = conn
        database.connect(conn)
    }

    override suspend fun disconnectSync() {
        database.disconnect()
    }

    override suspend fun disconnectAndClearSync() {
        database.disconnectAndClear()
        connector = null
    }

    override suspend fun pauseSync() {
        database.disconnect()
    }

    override suspend fun resumeSync() {
        val conn = connector ?: return
        database.connect(conn)
    }

    // endregion

}
