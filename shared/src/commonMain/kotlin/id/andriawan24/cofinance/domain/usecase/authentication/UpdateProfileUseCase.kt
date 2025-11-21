package id.andriawan24.cofinance.domain.usecase.authentication

import id.andriawan24.cofinance.data.repository.AuthenticationRepository
import id.andriawan24.cofinance.domain.model.request.UpdateProfileParam
import id.andriawan24.cofinance.domain.model.response.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class UpdateProfileUseCase(
    private val authenticationRepository: AuthenticationRepository
) {

    fun execute(param: UpdateProfileParam): Flow<Result<User>> = flow {
        try {
            val user = authenticationRepository.updateProfile(param)
            emit(Result.success(user))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
