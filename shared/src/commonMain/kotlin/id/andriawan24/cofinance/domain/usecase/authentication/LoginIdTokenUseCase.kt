package id.andriawan24.cofinance.domain.usecase.authentication

import id.andriawan24.cofinance.data.repository.AuthenticationRepository
import id.andriawan24.cofinance.domain.model.request.IdTokenParam
import id.andriawan24.cofinance.utils.ResultState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LoginIdTokenUseCase(private val repository: AuthenticationRepository) {
    fun execute(idTokenParam: IdTokenParam): Flow<ResultState<Boolean>> {
        return flow {
            emit(ResultState.Loading)
            try {
                repository.login(idTokenParam)
                emit(ResultState.Success(true))
            } catch (e: Exception) {
                emit(ResultState.Error(e))
            }
        }
    }
}