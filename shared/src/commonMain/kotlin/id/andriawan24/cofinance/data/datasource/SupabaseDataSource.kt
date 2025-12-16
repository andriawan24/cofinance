package id.andriawan24.cofinance.data.datasource

import id.andriawan24.cofinance.data.model.request.AccountRequest
import id.andriawan24.cofinance.data.model.request.AddTransactionRequest
import id.andriawan24.cofinance.data.model.request.GetTransactionsRequest
import id.andriawan24.cofinance.data.model.request.IdTokenRequest
import id.andriawan24.cofinance.data.model.request.UpdateBalanceRequest
import id.andriawan24.cofinance.data.model.request.UpdateProfileRequest
import id.andriawan24.cofinance.data.model.response.AccountResponse
import id.andriawan24.cofinance.data.model.response.TransactionResponse
import id.andriawan24.cofinance.utils.enums.TransactionType
import id.andriawan24.cofinance.utils.ext.orZero
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.modifyUser
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.math.max
import kotlin.time.ExperimentalTime


class SupabaseDataSource(private val supabase: SupabaseClient) {

    fun getUser(): UserInfo? = supabase.auth.currentUserOrNull()

    suspend fun fetchUser(): UserInfo {
        val userInfo = supabase.auth.retrieveUserForCurrentSession(updateSession = true)
        return userInfo
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

    suspend fun updateProfile(request: UpdateProfileRequest): UserInfo {
        val avatarUrl = when {
            request.avatarBytes != null && request.mimeType != null -> {
                uploadAvatar(request.avatarBytes, request.mimeType)
            }

            !request.currentAvatarUrl.isNullOrBlank() -> request.currentAvatarUrl
            else -> null
        }

        val metadata = buildJsonObject {
            put("name", request.name)
            avatarUrl?.let { put("avatar_url", it) }
        }

        return supabase.auth.modifyUser {
            data = metadata
        }
    }

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

    suspend fun increaseBalance(request: UpdateBalanceRequest): AccountResponse {
        val account = supabase.from(AccountResponse.TABLE_NAME)
            .select {
                filter { AccountResponse::id eq request.accountsId }
            }
            .decodeSingleOrNull<AccountResponse>()

        requireNotNull(account) {
            throw IllegalArgumentException("Account not found")
        }

        val balance = max(0, account.balance.orZero().plus(request.amount))

        return supabase.from(AccountResponse.TABLE_NAME)
            .update(update = { AccountResponse::balance setTo balance }) {
                select()
                filter {
                    AccountResponse::id eq request.accountsId
                }
            }
            .decodeSingle()
    }

    suspend fun reduceBalance(request: UpdateBalanceRequest): AccountResponse {
        val account = supabase.from(AccountResponse.TABLE_NAME)
            .select {
                filter {
                    AccountResponse::id eq request.accountsId
                }
            }
            .decodeSingleOrNull<AccountResponse>()

        requireNotNull(account) {
            throw IllegalArgumentException("Account not found")
        }

        val balance = max(0, account.balance.orZero().minus(request.amount))

        return supabase.from(AccountResponse.TABLE_NAME)
            .update(update = { AccountResponse::balance setTo balance }) {
                select()
                filter {
                    AccountResponse::id eq request.accountsId
                }
            }
            .decodeSingle()
    }

    @OptIn(ExperimentalTime::class)
    suspend fun getTransactions(request: GetTransactionsRequest): List<TransactionResponse> {
        val month = request.month
        val year = request.year

        val columns = Columns.raw(
            """
                *,
                sender:transactions_accounts_id_fkey(*),
                receiver:transactions_receiver_accounts_id_fkey(*)
            """.trimIndent()
        )

        val transactions = supabase.from(TransactionResponse.TABLE_NAME).select(columns = columns) {
            filter {
                if (month != null && year != null) {
                    val startDate = LocalDateTime(
                        year = year,
                        month = month,
                        day = 1,
                        hour = 0,
                        minute = 0,
                        second = 0,
                        nanosecond = 0
                    ).toInstant(TimeZone.currentSystemDefault())

                    val nextDate = startDate.plus(
                        value = 1,
                        unit = DateTimeUnit.MONTH,
                        timeZone = TimeZone.currentSystemDefault()
                    )

                    and {
                        gte(column = TransactionResponse.DATE_FIELD, value = startDate.toString())
                        lt(column = TransactionResponse.DATE_FIELD, value = nextDate.toString())
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

    private suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): String {
        val userId = supabase.auth.currentUserOrNull()?.id.orEmpty()
        require(userId.isNotBlank()) { "User is not logged in" }

        val normalizedMime = mimeType.lowercase()
        val extension = when {
            normalizedMime.contains("png") -> "png"
            normalizedMime.contains("webp") -> "webp"
            else -> "jpg"
        }

        val objectPath = "$userId-${Clock.System.now().toEpochMilliseconds()}.$extension"
        val bucket = supabase.storage.from(AVATAR_BUCKET)

        bucket.upload(
            path = objectPath,
            data = bytes,
            upsert = true,
            contentType = mimeType
        )

        return bucket.publicUrl(path = objectPath)
    }

    private companion object {
        const val AVATAR_BUCKET = "avatars"
    }
}