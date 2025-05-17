package id.andriawan24.cofinance.data.datasource

import id.andriawan24.cofinance.data.model.request.IdTokenRequest
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserInfo

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
}