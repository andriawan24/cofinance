package id.andriawan24.cofinance.domain.usecase

import id.andriawan24.cofinance.data.repository.AuthenticationRepository
import id.andriawan24.cofinance.domain.model.response.User
import io.github.aakira.napier.Napier
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