package id.andriawan.cofinance.domain.usecases.authentications

import id.andriawan.cofinance.data.repository.AuthenticationRepository
import id.andriawan.cofinance.data.repository.AuthenticationRepositoryImpl.Companion.log
import id.andriawan.cofinance.domain.model.response.User
import id.andriawan.cofinance.utils.ResultState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class UpdateProfileUseCase(private val authRepository: AuthenticationRepository) {
    fun execute(name: String, avatarBytes: ByteArray?): Flow<ResultState<User>> = flow {
        emit(ResultState.Loading)
        try {
            val user = authRepository.updateProfile(name, avatarBytes)
            emit(ResultState.Success(user))
        } catch (e: Exception) {
            emit(ResultState.Error(e))
        }
    }
}
