package id.andriawan.cofinance.domain.usecases.authentications

import id.andriawan.cofinance.data.repository.AuthenticationRepository
import id.andriawan.cofinance.domain.model.response.User
import id.andriawan.cofinance.utils.ResultState
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
