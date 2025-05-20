package id.andriawan24.cofinance.domain.usecase.authentication

import id.andriawan24.cofinance.data.repository.AuthenticationRepository
import id.andriawan24.cofinance.domain.model.response.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FetchUserUseCase(private val authRepository: AuthenticationRepository) {

    fun execute(): Flow<Result<User>> = flow {
        try {
            val user = authRepository.fetchUser()
            emit(Result.success(user))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}