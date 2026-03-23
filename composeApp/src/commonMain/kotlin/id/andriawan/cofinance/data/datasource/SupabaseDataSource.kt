package id.andriawan.cofinance.data.datasource

import id.andriawan.cofinance.data.model.request.AccountRequest
import id.andriawan.cofinance.data.model.request.AddTransactionRequest
import id.andriawan.cofinance.data.model.request.GetTransactionsRequest
import id.andriawan.cofinance.data.model.request.IdTokenRequest
import id.andriawan.cofinance.data.model.response.AccountResponse
import id.andriawan.cofinance.data.model.response.TransactionResponse
import id.andriawan.cofinance.utils.enums.TransactionType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.JsonPrimitive


class SupabaseDataSource(private val supabase: SupabaseClient) {

    // region Auth

    fun getUser(): UserInfo? = supabase.auth.currentUserOrNull()

    suspend fun fetchUser(): UserInfo {
        return supabase.auth.retrieveUserForCurrentSession(updateSession = true)
    }

    suspend fun login(idTokenRequest: IdTokenRequest) {
        supabase.auth.signInWith(IDToken) {
            idToken = idTokenRequest.idToken
            provider = Google
        }
    }

    suspend fun logout() {
        supabase.auth.signOut(scope = SignOutScope.GLOBAL)
    }

    // endregion

    // region Storage

    suspend fun uploadAvatar(userId: String, bytes: ByteArray): String {
        val bucket = supabase.storage.from("avatars")
        val path = "$userId/avatar.jpg"
        bucket.upload(path, bytes) { upsert = true }
        return bucket.publicUrl(path)
    }

    suspend fun updateUserMetadata(name: String, avatarUrl: String?): UserInfo {
        return supabase.auth.updateUser {
            data {
                put("name", JsonPrimitive(name))
                put("custom_name", JsonPrimitive(name))
                if (avatarUrl != null) {
                    put("custom_avatar_url", JsonPrimitive(avatarUrl))
                }
            }
        }
    }

    suspend fun updateCycleStartDay(day: Int): UserInfo {
        return supabase.auth.updateUser {
            data {
                put("cycle_start_day", JsonPrimitive(day))
            }
        }
    }

    // endregion

    // region Data (used by OnlineOnlyDatabase for web targets)

    suspend fun getAccounts(): List<AccountResponse> {
        return supabase.from(AccountResponse.TABLE_NAME)
            .select {
                order(
                    column = AccountResponse.NAME_FIELD,
                    order = Order.DESCENDING
                )
            }
            .decodeList<AccountResponse>()
    }

    suspend fun addAccount(request: AccountRequest): AccountResponse {
        val userId = supabase.auth.currentUserOrNull()?.id.orEmpty()
        val newRequest = request.copy(usersId = userId)

        return supabase.from(AccountResponse.TABLE_NAME)
            .insert(newRequest) { select() }
            .decodeSingle()
    }

    suspend fun getTransactions(request: GetTransactionsRequest): List<TransactionResponse> {
        val columns = Columns.raw(
            """
                *,
                sender:transactions_accounts_id_fkey(*),
                receiver:transactions_receiver_accounts_id_fkey(*)
            """.trimIndent()
        )

        val transactions = supabase.from(TransactionResponse.TABLE_NAME).select(columns = columns) {
            filter {
                if (request.startDate != null && request.endDate != null) {
                    and {
                        gte(column = TransactionResponse.DATE_FIELD, value = request.startDate)
                        lt(column = TransactionResponse.DATE_FIELD, value = request.endDate)
                    }
                }

                if (request.transactionId != null) {
                    eq(column = TransactionResponse.ID_FIELD, value = request.transactionId)
                    this@select.limit(1)
                }

                if (request.isDraft) {
                    eq(column = TransactionResponse.TRANSACTION_TYPE_FIELD, TransactionType.DRAFT)
                } else {
                    neq(column = TransactionResponse.TRANSACTION_TYPE_FIELD, TransactionType.DRAFT)
                }
            }

            order(
                column = TransactionResponse.DATE_FIELD,
                order = Order.DESCENDING
            )
        }

        return transactions.decodeList<TransactionResponse>()
    }

    suspend fun createTransaction(request: AddTransactionRequest): TransactionResponse {
        val userId = supabase.auth.currentUserOrNull()?.id.orEmpty()
        val newRequest = request.copy(usersId = userId)

        return supabase.from(table = TransactionResponse.TABLE_NAME)
            .upsert(value = newRequest) {
                select(
                    columns = Columns.raw(
                        """
                            *,
                            sender:transactions_accounts_id_fkey(*),
                            receiver:transactions_receiver_accounts_id_fkey(*)
                        """.trimIndent()
                    )
                )
            }
            .decodeSingle<TransactionResponse>()
    }

    // endregion
}
