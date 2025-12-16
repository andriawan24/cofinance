package id.andriawan24.cofinance.domain.usecase.authentication

import id.andriawan24.cofinance.data.repository.AuthenticationRepository
import id.andriawan24.cofinance.domain.model.request.UpdateProfileParam
import id.andriawan24.cofinance.domain.model.response.User
import id.andriawan24.cofinance.utils.ResultState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class UpdateProfileUseCase(
    private val authenticationRepository: AuthenticationRepository
) {
    fun execute(param: UpdateProfileParam): Flow<ResultState<User>> = flow {
        emit(ResultState.Loading)
        try {
            val user = authenticationRepository.updateProfile(param)
            emit(ResultState.Success(user))
        } catch (e: Exception) {
            emit(ResultState.Error(e))
        }
    }
}
