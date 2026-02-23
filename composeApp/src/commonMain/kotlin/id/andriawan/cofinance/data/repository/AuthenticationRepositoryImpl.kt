package id.andriawan.cofinance.data.repository

import com.diamondedge.logging.logging
import id.andriawan.cofinance.data.datasource.SupabaseDataSource
import id.andriawan.cofinance.domain.model.request.IdTokenParam
import id.andriawan.cofinance.domain.model.request.IdTokenParam.Companion.toRequest
import id.andriawan.cofinance.domain.model.response.User
import id.andriawan.cofinance.pages.activity.ActivityViewModel


interface AuthenticationRepository {
    fun getUser(): User
    suspend fun fetchUser(): User
    suspend fun login(idTokenParam: IdTokenParam)
    suspend fun logout()
    suspend fun updateProfile(name: String, avatarBytes: ByteArray?): User
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

    override suspend fun updateProfile(name: String, avatarBytes: ByteArray?): User {
        var avatarUrl: String? = null
        if (avatarBytes != null) {
            val userId = supabaseDataSource.getUser()?.id.orEmpty()
            avatarUrl = supabaseDataSource.uploadAvatar(userId, avatarBytes)
        }
        val userInfo = supabaseDataSource.updateUserMetadata(name, avatarUrl)
        return User.from(userInfo)
    }

    companion object {
        val log = logging(AuthenticationRepositoryImpl::class.simpleName)
    }
}
