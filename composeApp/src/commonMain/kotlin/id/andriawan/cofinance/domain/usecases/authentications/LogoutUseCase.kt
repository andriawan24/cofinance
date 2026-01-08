package id.andriawan.cofinance.domain.usecases.authentications

import id.andriawan.cofinance.data.repository.AuthenticationRepository
import id.andriawan.cofinance.utils.ResultState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LogoutUseCase(private val authRepository: AuthenticationRepository) {
    fun execute(): Flow<ResultState<Boolean>> = flow {
        emit(ResultState.Loading)
        try {
            authRepository.logout()
            emit(ResultState.Success(true))
        } catch (e: Exception) {
            emit(ResultState.Error(e))
        }
    }
}
