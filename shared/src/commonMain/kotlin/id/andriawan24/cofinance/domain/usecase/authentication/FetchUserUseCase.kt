package id.andriawan24.cofinance.domain.usecase.authentication

import id.andriawan24.cofinance.data.repository.AuthenticationRepository
import id.andriawan24.cofinance.domain.model.response.User
import id.andriawan24.cofinance.utils.ResultState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow


class FetchUserUseCase(private val authRepository: AuthenticationRepository) {

    fun execute(): Flow<ResultState<User>> = flow {
        emit(ResultState.Loading)
        try {
            val user = authRepository.fetchUser()
            emit(ResultState.Success(user))
        } catch (e: Exception) {
            emit(ResultState.Error(e))
        }
    }
}