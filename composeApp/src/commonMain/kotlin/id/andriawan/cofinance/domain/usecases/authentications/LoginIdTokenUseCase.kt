package id.andriawan.cofinance.domain.usecases.authentications

import id.andriawan.cofinance.data.repository.AuthenticationRepository
import id.andriawan.cofinance.domain.model.request.IdTokenParam
import id.andriawan.cofinance.utils.ResultState
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
