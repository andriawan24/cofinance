package id.andriawan.cofinance.pages.splash

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import id.andriawan.cofinance.domain.model.response.User
import id.andriawan.cofinance.domain.usecases.authentications.FetchUserUseCase
import id.andriawan.cofinance.utils.ResultState
import dev.gitlive.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Stable
class SplashViewModel(
    private val fetchUserUseCase: FetchUserUseCase,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    fun fetchUser(): Flow<ResultState<Boolean>> = flow {
        emit(ResultState.Loading)

        if (firebaseAuth.currentUser == null) {
            emit(ResultState.Error(Exception("No active session")))
            return@flow
        }

        try {
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
            emit(ResultState.Error(e))
        }
    }
}
