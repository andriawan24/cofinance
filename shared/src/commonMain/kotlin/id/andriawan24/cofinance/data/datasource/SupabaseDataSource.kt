package id.andriawan24.cofinance.data.datasource

import id.andriawan24.cofinance.data.model.request.AccountRequest
import id.andriawan24.cofinance.data.model.request.IdTokenRequest
import id.andriawan24.cofinance.data.model.request.TransactionRequest
import id.andriawan24.cofinance.data.model.response.AccountResponse
import id.andriawan24.cofinance.data.model.response.TransactionResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.from

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
            .insert(newRequest) {
                select()
            }
            .decodeSingle<AccountResponse>()
    }

    suspend fun createTransaction(request: TransactionRequest): TransactionResponse {
        val userId = supabase.auth.currentUserOrNull()?.id.orEmpty()
        val newRequest = request.copy(usersId = userId)

        return supabase.from(table = TransactionResponse.TABLE_NAME)
            .insert(value = newRequest) {
                select()
            }
            .decodeSingle<TransactionResponse>()
    }
}