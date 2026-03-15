package id.andriawan.cofinance.data.powersync

import com.diamondedge.logging.logging
import com.powersync.PowerSyncDatabase
import com.powersync.connectors.PowerSyncBackendConnector
import com.powersync.connectors.PowerSyncCredentials
import com.powersync.db.crud.CrudEntry
import com.powersync.db.crud.UpdateType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class CofinanceConnector(
    private val supabaseClient: SupabaseClient,
    private val powerSyncUrl: String
) : PowerSyncBackendConnector() {

    private val log = logging("PowerSyncConnector")

    override suspend fun fetchCredentials(): PowerSyncCredentials {
        val session = supabaseClient.auth.currentSessionOrNull()

        if (session == null) {
            log.error { "fetchCredentials: No active Supabase session" }
            error("No active Supabase session")
        }

        log.info {
            "fetchCredentials: endpoint=$powerSyncUrl, " +
                "tokenLength=${session.accessToken.length}, " +
                "userId=${session.user?.id}"
        }

        return PowerSyncCredentials(
            endpoint = powerSyncUrl,
            token = session.accessToken
        )
    }

    override suspend fun uploadData(database: PowerSyncDatabase) {
        val transaction = database.getNextCrudTransaction() ?: return

        log.info { "uploadData: ${transaction.crud.size} entries to upload" }

        try {
            for (entry in transaction.crud) {
                log.info { "uploadData: ${entry.op} ${entry.table} id=${entry.id}" }
                uploadCrudEntry(entry)
            }

            transaction.complete(null)
            log.info { "uploadData: completed successfully" }
        } catch (e: Exception) {
            log.error { "uploadData error: ${e.message}" }
            if (isNonRetryableError(e)) {
                transaction.complete(null)
            } else {
                throw e
            }
        }
    }

    private suspend fun uploadCrudEntry(entry: CrudEntry) {
        when (entry.table) {
            "transactions" -> uploadTransaction(entry)
            "accounts" -> uploadAccount(entry)
            else -> uploadGeneric(entry)
        }
    }

    private suspend fun uploadTransaction(entry: CrudEntry) {
        when (entry.op) {
            UpdateType.PUT -> {
                val data = entry.opData ?: return
                val type = data["type"]

                if (type == "TRANSFER") {
                    supabaseClient.postgrest.rpc(
                        function = "execute_transfer",
                        parameters = buildJsonObject {
                            put("p_transaction_id", entry.id)
                            put("p_sender_id", data["accounts_id"])
                            put("p_receiver_id", data["receiver_accounts_id"])
                            put("p_amount", data["amount"]?.toLongOrNull() ?: 0L)
                            put("p_fee", data["fee"]?.toLongOrNull() ?: 0L)
                            put("p_category", data["category"])
                            put("p_notes", data["notes"])
                            put("p_date", data["date"])
                            put("p_user_id", data["users_id"])
                        }
                    )
                } else {
                    val insertData = buildMap {
                        put("id", JsonPrimitive(entry.id))
                        entry.opData?.jsonValues?.let { putAll(it) }
                    }

                    supabaseClient.from("transactions").upsert(insertData)

                    val accountsId = data["accounts_id"]
                    val amount = data["amount"]?.toLongOrNull() ?: 0L
                    val fee = data["fee"]?.toLongOrNull() ?: 0L

                    if (accountsId != null) {
                        val delta = when (type) {
                            "EXPENSE" -> -(amount + fee)
                            "INCOME" -> amount
                            else -> 0L
                        }

                        if (delta != 0L) {
                            supabaseClient.postgrest.rpc(
                                function = "adjust_balance",
                                parameters = buildJsonObject {
                                    put("p_account_id", accountsId)
                                    put("p_delta", delta)
                                }
                            )
                        }
                    }
                }
            }

            UpdateType.PATCH -> {
                supabaseClient.from("transactions")
                    .update(entry.opData!!.jsonValues) {
                        filter { eq("id", entry.id) }
                    }
            }

            UpdateType.DELETE -> {
                supabaseClient.from("transactions")
                    .delete {
                        filter { eq("id", entry.id) }
                    }
            }
        }
    }

    private suspend fun uploadAccount(entry: CrudEntry) {
        when (entry.op) {
            UpdateType.PUT -> {
                val data = buildMap {
                    put("id", JsonPrimitive(entry.id))
                    entry.opData?.jsonValues?.let { putAll(it) }
                }
                supabaseClient.from("accounts").upsert(data)
            }

            UpdateType.PATCH -> {
                supabaseClient.from("accounts")
                    .update(entry.opData!!.jsonValues) {
                        filter { eq("id", entry.id) }
                    }
            }

            UpdateType.DELETE -> {
                supabaseClient.from("accounts")
                    .delete {
                        filter { eq("id", entry.id) }
                    }
            }
        }
    }

    private suspend fun uploadGeneric(entry: CrudEntry) {
        val table = supabaseClient.from(entry.table)
        when (entry.op) {
            UpdateType.PUT -> {
                val data = buildMap {
                    put("id", JsonPrimitive(entry.id))
                    entry.opData?.jsonValues?.let { putAll(it) }
                }
                table.upsert(data)
            }

            UpdateType.PATCH -> {
                table.update(entry.opData!!.jsonValues) {
                    filter { eq("id", entry.id) }
                }
            }

            UpdateType.DELETE -> {
                table.delete {
                    filter { eq("id", entry.id) }
                }
            }
        }
    }

    private fun isNonRetryableError(e: Exception): Boolean {
        val message = e.message ?: return false
        return message.contains("40") && !message.contains("429")
    }
}
