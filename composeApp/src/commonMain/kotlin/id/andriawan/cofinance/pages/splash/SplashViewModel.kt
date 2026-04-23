package id.andriawan.cofinance.pages.splash

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.andriawan.cofinance.BuildKonfig
import id.andriawan.cofinance.data.local.CofinanceDatabase
import id.andriawan.cofinance.domain.model.response.User
import id.andriawan.cofinance.domain.usecases.authentications.FetchUserUseCase
import id.andriawan.cofinance.utils.ResultState
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Stable
class SplashViewModel(
    private val fetchUserUseCase: FetchUserUseCase,
    private val database: CofinanceDatabase,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    fun fetchUser(): Flow<ResultState<Boolean>> = flow {
        emit(ResultState.Loading)

        supabaseClient.auth.loadFromStorage(autoRefresh = true)
        supabaseClient.auth.awaitInitialization()

        when (supabaseClient.auth.sessionStatus.value) {
            is SessionStatus.NotAuthenticated,
            is SessionStatus.RefreshFailure -> {
                emit(ResultState.Error(Exception("No active session")))
                return@flow
            }

            else -> Unit
        }

        try {
            database.connectSync(supabaseClient, BuildKonfig.POWERSYNC_URL)
        } catch (_: Exception) {
            // Sync connection failure is non-fatal
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
