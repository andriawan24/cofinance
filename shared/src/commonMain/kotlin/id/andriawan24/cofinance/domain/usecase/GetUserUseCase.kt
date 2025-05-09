package id.andriawan24.cofinance.domain.usecase

import id.andriawan24.cofinance.data.repository.AuthenticationRepository
import id.andriawan24.cofinance.domain.model.response.User
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetUserUseCase(private val authRepository: AuthenticationRepository) {

    fun execute(): Flow<Result<User>> = flow {
        val user = authRepository.getUser()
        emit(Result.success(user))
    }
}