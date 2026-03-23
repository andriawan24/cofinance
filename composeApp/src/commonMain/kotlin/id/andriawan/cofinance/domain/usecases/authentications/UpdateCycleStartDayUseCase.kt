package id.andriawan.cofinance.domain.usecases.authentications

import id.andriawan.cofinance.data.repository.AuthenticationRepository
import id.andriawan.cofinance.domain.model.response.User
import id.andriawan.cofinance.utils.ResultState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class UpdateCycleStartDayUseCase(private val authRepository: AuthenticationRepository) {
    fun execute(day: Int): Flow<ResultState<User>> = flow {
        emit(ResultState.Loading)
        try {
            val user = authRepository.updateCycleStartDay(day)
            emit(ResultState.Success(user))
        } catch (e: Exception) {
            emit(ResultState.Error(e))
        }
    }
}
