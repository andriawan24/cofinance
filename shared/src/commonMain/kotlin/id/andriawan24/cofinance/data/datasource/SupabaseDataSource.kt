package id.andriawan24.cofinance.data.datasource

import id.andriawan24.cofinance.data.model.request.IdTokenRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserInfo

class SupabaseDataSource(private val supabase: SupabaseClient) {

    suspend fun getUser(): UserInfo {
        return supabase.auth.retrieveUserForCurrentSession(updateSession = true)
    }

    suspend fun signInWithIdToken(idTokenRequest: IdTokenRequest) {
        supabase.auth.signInWith(IDToken) {
            idToken = idTokenRequest.idToken
            provider = Google
        }
    }
}