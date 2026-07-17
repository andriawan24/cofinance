package id.andriawan.cofinance.data.repository

import id.andriawan.cofinance.data.datasource.FirebaseDataSource
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
    suspend fun updateCycleStartDay(day: Int): User
    suspend fun updateLastCycleResetDate(date: String): User
}


class AuthenticationRepositoryImpl(
    private val firebaseDataSource: FirebaseDataSource
) : AuthenticationRepository {

    override fun getUser(): User = User.from(firebaseDataSource.getUser())

    override suspend fun fetchUser(): User {
        val user = firebaseDataSource.fetchUser()
        return User.from(user)
    }

    override suspend fun login(idTokenParam: IdTokenParam) {
        firebaseDataSource.login(idTokenParam.toRequest())
    }

    override suspend fun logout() {
        firebaseDataSource.logout()
    }

    override suspend fun updateProfile(name: String, avatarBytes: ByteArray?): User {
        var avatarUrl: String? = null
        if (avatarBytes != null) {
            val userId = firebaseDataSource.getUser()?.id.orEmpty()
            avatarUrl = firebaseDataSource.uploadAvatar(userId, avatarBytes)
        }
        val userInfo = firebaseDataSource.updateUserMetadata(name, avatarUrl)
        return User.from(userInfo)
    }

    override suspend fun updateCycleStartDay(day: Int): User {
        val userInfo = firebaseDataSource.updateCycleStartDay(day)
        return User.from(userInfo)
    }

    override suspend fun updateLastCycleResetDate(date: String): User {
        val userInfo = firebaseDataSource.updateLastCycleResetDate(date)
        return User.from(userInfo)
    }
}
