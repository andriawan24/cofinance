package id.andriawan.cofinance.pages.profile

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diamondedge.logging.logging
import id.andriawan.cofinance.data.datasource.ModelStatus
import id.andriawan.cofinance.data.datasource.ReceiptScannerService
import id.andriawan.cofinance.data.local.CofinanceDatabase
import id.andriawan.cofinance.domain.usecases.authentications.GetUserUseCase
import id.andriawan.cofinance.domain.usecases.authentications.LogoutUseCase
import id.andriawan.cofinance.pages.profile.ProfileEvent.NavigateToLoginPage
import id.andriawan.cofinance.pages.profile.ProfileEvent.ShowMessage
import id.andriawan.cofinance.utils.ResultState
import id.andriawan.cofinance.utils.UiText
import id.andriawan.cofinance.utils.mapAuthErrorMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


sealed class ProfileEvent {
    data object NavigateToLoginPage : ProfileEvent()
    data class ShowMessage(val message: UiText) : ProfileEvent()
}

data class UiState(
    val isShowDialogLogout: Boolean = false,
    val isShowDeleteModelDialog: Boolean = false
)

@Stable
class ProfileViewModel(
    private val getUserUseCase: GetUserUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val database: CofinanceDatabase,
    private val receiptScanner: ReceiptScannerService
) : ViewModel() {

    private val _profileEvent = Channel<ProfileEvent>(Channel.BUFFERED)
    val profileEvent = _profileEvent.receiveAsFlow()

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private val _user = MutableStateFlow(getUserUseCase.execute())
    val user = _user.asStateFlow()

    val modelStatus = receiptScanner.getModelStatus()

    fun refreshUser() {
        _user.value = getUserUseCase.execute()
    }

    fun toggleDialogLogout(isShow: Boolean) {
        _uiState.update { state -> state.copy(isShowDialogLogout = isShow) }
    }

    fun downloadModel() {
        viewModelScope.launch {
            try {
                receiptScanner.downloadModel()
            } catch (e: Exception) {
                _profileEvent.send(ShowMessage(UiText.Raw(e.message ?: "Download failed")))
            }
        }
    }

    fun toggleDeleteModelDialog(isShow: Boolean) {
        _uiState.update { it.copy(isShowDeleteModelDialog = isShow) }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                database.disconnectSync()
            } catch (_: Exception) {
                /* no-op */
            }

            logoutUseCase.execute().collectLatest {
                when (it) {
                    ResultState.Loading -> {
                        /* no-op */
                    }

                    is ResultState.Error -> {
                        _profileEvent.send(ShowMessage(mapAuthErrorMessage(it.exception)))
                    }

                    is ResultState.Success<*> -> {
                        _profileEvent.send(NavigateToLoginPage)
                    }
                }
            }
        }
    }
}
