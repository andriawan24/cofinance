package id.andriawan24.cofinance.data.repository

import id.andriawan24.cofinance.data.datasource.SupabaseDataSource
import id.andriawan24.cofinance.domain.model.IdTokenParam
import id.andriawan24.cofinance.domain.model.IdTokenParam.Companion.toRequest
import io.github.jan.supabase.auth.user.UserInfo

class AuthenticationRepository(private val supabaseDataSource: SupabaseDataSource) {

    suspend fun getUser(): UserInfo {
        return supabaseDataSource.getUser()
    }

    suspend fun signInWithIdToken(idTokenParam: IdTokenParam) {
        supabaseDataSource.signInWithIdToken(idTokenParam.toRequest())
    }
}