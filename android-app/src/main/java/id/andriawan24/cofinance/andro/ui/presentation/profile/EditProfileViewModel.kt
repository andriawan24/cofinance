package id.andriawan24.cofinance.andro.ui.presentation.profile

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.andriawan24.cofinance.andro.utils.emptyString
import id.andriawan24.cofinance.domain.model.request.UpdateProfileParam
import id.andriawan24.cofinance.domain.usecase.authentication.GetUserUseCase
import id.andriawan24.cofinance.domain.usecase.authentication.UpdateProfileUseCase
import id.andriawan24.cofinance.utils.ResultState
import java.io.IOException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditProfileUiState(
    val name: String = emptyString(),
    val avatarUrl: String = emptyString(),
    val localAvatarUri: Uri? = null,
    val isLoading: Boolean = false,
    val canSave: Boolean = false
)

sealed interface EditProfileEvent {
    data class ShowMessage(val message: String) : EditProfileEvent
    data object EmptyName : EditProfileEvent
    data object ProfileUpdated : EditProfileEvent
}

class EditProfileViewModel(
    getUserUseCase: GetUserUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase
) : ViewModel() {

    private val initialUser = getUserUseCase.execute()
    private var baselineName: String = initialUser.name
    private var currentAvatarUrl: String? = initialUser.avatarUrl.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(
        EditProfileUiState(
            name = initialUser.name,
            avatarUrl = initialUser.avatarUrl
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<EditProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var avatarBytes: ByteArray? = null
    private var mimeType: String? = null

    fun onNameChanged(name: String) {
        _uiState.update {
            it.copy(
                name = name,
                canSave = canSave(name = name)
            )
        }
    }

    fun onAvatarSelected(contentResolver: ContentResolver, imageUri: Uri) {
        viewModelScope.launch {
            try {
                val bytes = contentResolver.openInputStream(imageUri)?.use { input ->
                    input.readBytes()
                } ?: throw IOException("Unable to read the selected image")

                val detectedMimeType = contentResolver.getType(imageUri) ?: "image/jpeg"
                avatarBytes = bytes
                mimeType = detectedMimeType

                _uiState.update {
                    it.copy(
                        localAvatarUri = imageUri,
                        canSave = canSave(name = it.name, avatarChanged = true)
                    )
                }
            } catch (e: Exception) {
                _events.send(EditProfileEvent.ShowMessage(e.message.orEmpty()))
            }
        }
    }

    fun saveProfile() {
        val trimmedName = _uiState.value.name.trim()
        if (trimmedName.isBlank()) {
            viewModelScope.launch {
                _events.send(EditProfileEvent.EmptyName)
            }
            return
        }

        if (!_uiState.value.canSave || _uiState.value.isLoading) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val request = UpdateProfileParam(
                name = trimmedName,
                avatarBytes = avatarBytes,
                mimeType = mimeType,
                currentAvatarUrl = currentAvatarUrl
            )

            updateProfileUseCase.execute(request).collectLatest { result ->
                when (result) {
                    ResultState.Loading -> {
                        // Loading state already handled
                    }

                    is ResultState.Success -> {
                        val updatedUser = result.data
                        baselineName = updatedUser.name
                        currentAvatarUrl = updatedUser.avatarUrl.takeIf { it.isNotBlank() }
                        avatarBytes = null
                        mimeType = null

                        _uiState.update {
                            it.copy(
                                name = updatedUser.name,
                                avatarUrl = updatedUser.avatarUrl,
                                localAvatarUri = null,
                                isLoading = false,
                                canSave = false
                            )
                        }
                        _events.send(EditProfileEvent.ProfileUpdated)
                    }

                    is ResultState.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        _events.send(EditProfileEvent.ShowMessage(result.exception.message.orEmpty()))
                    }
                }
            }
        }
    }

    private fun canSave(name: String, avatarChanged: Boolean = avatarBytes != null): Boolean {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return false

        val nameChanged = trimmedName != baselineName.trim()
        return nameChanged || avatarChanged
    }
}
