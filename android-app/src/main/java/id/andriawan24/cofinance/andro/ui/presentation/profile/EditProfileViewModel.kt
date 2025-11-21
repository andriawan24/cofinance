package id.andriawan24.cofinance.andro.ui.presentation.profile

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan24.cofinance.domain.model.request.UpdateProfileParam
import id.andriawan24.cofinance.domain.usecase.authentication.GetUserUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.UpdateProfileUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditProfileUiState(
    val name: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val selectedAvatarUri: Uri? = null,
    val isSaving: Boolean = false,
    val isValid: Boolean = false
)

sealed class EditProfileEvent {
    data class ShowMessage(val message: String) : EditProfileEvent()
    data object ProfileSaved : EditProfileEvent()
    data object NameRequired : EditProfileEvent()
}

class EditProfileViewModel(
    getUserUseCase: GetUserUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase
) : ViewModel() {

    private val currentUser = getUserUseCase.execute()

    private val _uiState = MutableStateFlow(
        EditProfileUiState(
            name = currentUser.name,
            email = currentUser.email,
            avatarUrl = currentUser.avatarUrl,
            isValid = currentUser.name.isNotBlank()
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<EditProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var avatarBytes: ByteArray? = null
    private var avatarMimeType: String? = null

    fun onNameChanged(name: String) {
        _uiState.update {
            it.copy(name = name, isValid = name.isNotBlank())
        }
    }

    fun onAvatarSelected(contentResolver: ContentResolver, imageUri: Uri) {
        viewModelScope.launch {
            runCatching {
                contentResolver.openInputStream(imageUri)?.use { stream ->
                    stream.readBytes()
                } ?: error("Unable to read selected image")
            }.onSuccess { bytes ->
                avatarBytes = bytes
                avatarMimeType = contentResolver.getType(imageUri) ?: DEFAULT_MIME_TYPE
                _uiState.update { it.copy(selectedAvatarUri = imageUri) }
            }.onFailure {
                avatarBytes = null
                avatarMimeType = null
                _uiState.update { it.copy(selectedAvatarUri = null) }
                _events.send(EditProfileEvent.ShowMessage(it.message ?: "Failed to load image"))
            }
        }
    }

    fun saveProfile() {
        val name = uiState.value.name.trim()
        if (name.isBlank()) {
            viewModelScope.launch {
                _events.send(EditProfileEvent.NameRequired)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            val param = UpdateProfileParam(
                name = name,
                currentAvatarUrl = uiState.value.avatarUrl,
                avatarBytes = avatarBytes,
                mimeType = avatarMimeType
            )

            updateProfileUseCase.execute(param).collectLatest { result ->
                if (result.isSuccess) {
                    val updatedUser = result.getOrNull()
                    avatarBytes = null
                    avatarMimeType = null
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            name = updatedUser?.name ?: name,
                            email = updatedUser?.email ?: it.email,
                            avatarUrl = updatedUser?.avatarUrl.orEmpty(),
                            selectedAvatarUri = null,
                            isValid = true
                        )
                    }
                    _events.send(EditProfileEvent.ProfileSaved)
                } else {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(
                        EditProfileEvent.ShowMessage(
                            result.exceptionOrNull()?.message.orEmpty()
                        )
                    )
                }
            }
        }
    }

    companion object {
        private const val DEFAULT_MIME_TYPE = "image/jpeg"
    }
}
