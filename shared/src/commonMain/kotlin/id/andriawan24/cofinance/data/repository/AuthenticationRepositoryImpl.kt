package id.andriawan24.cofinance.data.repository

import id.andriawan24.cofinance.data.datasource.SupabaseDataSource
import id.andriawan24.cofinance.domain.model.request.IdTokenParam
import id.andriawan24.cofinance.domain.model.request.IdTokenParam.Companion.toRequest
import id.andriawan24.cofinance.domain.model.request.UpdateProfileParam
import id.andriawan24.cofinance.domain.model.request.UpdateProfileParam.Companion.toRequest
import id.andriawan24.cofinance.domain.model.response.User


interface AuthenticationRepository {
    fun getUser(): User
    suspend fun fetchUser(): User
    suspend fun login(idTokenParam: IdTokenParam)
    suspend fun logout()
    suspend fun updateProfile(param: UpdateProfileParam): User
}


class AuthenticationRepositoryImpl(
    private val supabaseDataSource: SupabaseDataSource
) : AuthenticationRepository {

    override fun getUser(): User = User.from(supabaseDataSource.getUser())

    override suspend fun fetchUser(): User {
        val user = supabaseDataSource.fetchUser()
        return User.from(user)
    }

    override suspend fun login(idTokenParam: IdTokenParam) {
        supabaseDataSource.login(idTokenParam.toRequest())
    }

    override suspend fun logout() {
        supabaseDataSource.logout()
    }

    override suspend fun updateProfile(param: UpdateProfileParam): User {
        val updatedUser = supabaseDataSource.updateProfile(param.toRequest())
        return User.from(updatedUser)
    }
}