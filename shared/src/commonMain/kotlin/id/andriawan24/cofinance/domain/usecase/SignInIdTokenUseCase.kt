package id.andriawan24.cofinance.domain.usecase

import id.andriawan24.cofinance.data.repository.AuthenticationRepository
import id.andriawan24.cofinance.domain.model.IdTokenParam
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SignInIdTokenUseCase(private val repository: AuthenticationRepository) {
    fun execute(idTokenParam: IdTokenParam): Flow<Result<Boolean>> {
        return flow {
            try {
                repository.signInWithIdToken(idTokenParam)
                emit(Result.success(true))
            } catch (e: Exception) {
                emit(Result.failure(e))
            }
        }
    }
}