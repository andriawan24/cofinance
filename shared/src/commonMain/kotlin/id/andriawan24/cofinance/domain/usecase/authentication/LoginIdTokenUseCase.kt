package id.andriawan24.cofinance.domain.usecase.authentication

import id.andriawan24.cofinance.data.repository.AuthenticationRepository
import id.andriawan24.cofinance.domain.model.request.IdTokenParam
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LoginIdTokenUseCase(private val repository: AuthenticationRepository) {
    fun execute(idTokenParam: IdTokenParam): Flow<Result<Boolean>> {
        return flow {
            try {
                repository.login(idTokenParam)
                emit(Result.success(true))
            } catch (e: Exception) {
                emit(Result.failure(e))
            }
        }
    }
}