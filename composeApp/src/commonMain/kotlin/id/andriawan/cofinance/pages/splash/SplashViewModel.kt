package id.andriawan.cofinance.pages.splash

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import id.andriawan.cofinance.domain.model.response.User
import id.andriawan.cofinance.domain.usecases.authentications.FetchUserUseCase
import id.andriawan.cofinance.utils.ResultState
import id.andriawan.cofinance.data.repository.AuthenticationRepository
import id.andriawan.cofinance.data.sync.FinanceSyncCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Stable
class SplashViewModel(
    private val fetchUserUseCase: FetchUserUseCase,
    private val authenticationRepository: AuthenticationRepository,
    private val syncCoordinator: FinanceSyncCoordinator
) : ViewModel() {

    fun fetchUser(): Flow<ResultState<Boolean>> = flow {
        emit(ResultState.Loading)

        if (!authenticationRepository.isSignedIn()) {
            emit(ResultState.Success(true))
            return@flow
        }

        try {
            syncCoordinator.syncAfterSignIn()
            fetchUserUseCase.execute().collect { result ->
                when (result) {
                    is ResultState.Success<User> -> emit(ResultState.Success(true))
                    is ResultState.Error -> emit(ResultState.Error(result.exception))
                    ResultState.Loading -> {
                        /* no-op */
                    }
                }
            }
        } catch (e: Exception) {
            // Startup remains local-first if the authenticated profile is temporarily offline.
            emit(ResultState.Success(true))
        }
    }
}
