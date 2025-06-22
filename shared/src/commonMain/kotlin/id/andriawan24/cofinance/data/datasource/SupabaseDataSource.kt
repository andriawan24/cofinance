package id.andriawan24.cofinance.data.datasource

import id.andriawan24.cofinance.data.model.request.AccountRequest
import id.andriawan24.cofinance.data.model.request.AddTransactionRequest
import id.andriawan24.cofinance.data.model.request.GetTransactionsRequest
import id.andriawan24.cofinance.data.model.request.IdTokenRequest
import id.andriawan24.cofinance.data.model.response.AccountResponse
import id.andriawan24.cofinance.data.model.response.TransactionResponse
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus

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

    suspend fun getAccounts(): List<AccountResponse> {
        return supabase.from(AccountResponse.TABLE_NAME)
            .select()
            .decodeList<AccountResponse>()
    }

    suspend fun addAccount(request: AccountRequest): AccountResponse {
        val userId = supabase.auth.currentUserOrNull()?.id.orEmpty()
        val newRequest = request.copy(usersId = userId)

        return supabase.from(AccountResponse.TABLE_NAME)
            .insert(newRequest) { select() }
            .decodeSingle<AccountResponse>()
    }

    suspend fun getTransactions(request: GetTransactionsRequest): List<TransactionResponse> {
        val month = request.month
        val year = request.year

        val columns = Columns.raw(
            """
                *,
                accounts (
                    *
                )
            """.trimIndent()
        )

        val transactions = supabase.from(TransactionResponse.TABLE_NAME).select(columns = columns) {
            filter {
                if (month != null && year != null) {
                    val startOfMonth = LocalDateTime(year, month, 1, 0, 0)

                    val firstDayOfMonth = LocalDate(year, month, 1)
                    val startOfNextMonth = firstDayOfMonth.plus(1, DateTimeUnit.MONTH)
                        .let { LocalDateTime(it.year, it.monthNumber, it.dayOfMonth, 0, 0) }

                    val start = startOfMonth.toString()
                    val endExclusive = startOfNextMonth.toString()

                    and {
                        gte(TransactionResponse.DATE_FIELD, start)
                        lt(TransactionResponse.DATE_FIELD, endExclusive)
                    }
                }
            }
        }

        Napier.d { "Test debug transaction ${transactions.data}" }

        return transactions.decodeList<TransactionResponse>()
    }

    suspend fun createTransaction(request: AddTransactionRequest): TransactionResponse {
        val userId = supabase.auth.currentUserOrNull()?.id.orEmpty()
        val newRequest = request.copy(usersId = userId)

        return supabase.from(table = TransactionResponse.TABLE_NAME)
            .insert(value = newRequest) {
                select()
            }
            .decodeSingle<TransactionResponse>()
    }
}