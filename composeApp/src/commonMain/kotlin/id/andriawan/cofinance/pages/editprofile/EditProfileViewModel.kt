package id.andriawan.cofinance.pages.editprofile

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan.cofinance.domain.usecases.authentications.GetUserUseCase
import id.andriawan.cofinance.domain.usecases.authentications.UpdateProfileUseCase
import id.andriawan.cofinance.utils.ResultState
import id.andriawan.cofinance.utils.mapAuthErrorMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class EditProfileEvent {
    data object ProfileUpdated : EditProfileEvent()
    data class ShowError(val message: String) : EditProfileEvent()
}

data class EditProfileUiState(
    val name: String = "",
    val avatarUrl: String = "",
    val localImageUri: String? = null,
    val imageBytes: ByteArray? = null,
    val isLoading: Boolean = false
)

@Stable
class EditProfileViewModel(
    getUserUseCase: GetUserUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase
) : ViewModel() {

    private val _event = Channel<EditProfileEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val user = getUserUseCase.execute()
        _uiState.update {
            it.copy(name = user.name, avatarUrl = user.avatarUrl)
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onImageSelected(uri: String, bytes: ByteArray) {
        _uiState.update { it.copy(localImageUri = uri, imageBytes = bytes) }
    }

    fun saveProfile() {
        viewModelScope.launch {
            val state = _uiState.value
            updateProfileUseCase.execute(state.name, state.imageBytes).collectLatest {
                when (it) {
                    ResultState.Loading -> {
                        _uiState.update { s -> s.copy(isLoading = true) }
                    }

                    is ResultState.Success -> {
                        _uiState.update { s -> s.copy(isLoading = false) }
                        _event.send(EditProfileEvent.ProfileUpdated)
                    }

                    is ResultState.Error -> {
                        _uiState.update { s -> s.copy(isLoading = false) }
                        _event.send(EditProfileEvent.ShowError(mapAuthErrorMessage(it.exception)))
                    }
                }
            }
        }
    }
}
