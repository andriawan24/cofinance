package id.andriawan24.cofinance.andro.ui.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.components.PrimaryButton
import id.andriawan24.cofinance.andro.ui.components.SecondaryButton
import id.andriawan24.cofinance.andro.ui.components.VerticalSpacing
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.Dimensions
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    onProfileUpdated: () -> Unit,
    editProfileViewModel: EditProfileViewModel = koinViewModel()
) {
    val uiState by editProfileViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val contentResolver = remember(context) { context.contentResolver }
    val snackbarHostState = remember { SnackbarHostState() }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { editProfileViewModel.onAvatarSelected(contentResolver, it) }
    }

    LaunchedEffect(editProfileViewModel) {
        editProfileViewModel.events.collectLatest { event ->
            when (event) {
                is EditProfileEvent.ShowMessage -> {
                    val message = event.message.ifBlank {
                        context.getString(R.string.error_generic)
                    }
                    snackbarHostState.showSnackbar(message)
                }

                EditProfileEvent.NameRequired -> {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.error_name_required)
                    )
                }

                EditProfileEvent.ProfileSaved -> {
                    onProfileUpdated()
                    onNavigateBack()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            EditProfileTopBar(onNavigateBack = onNavigateBack)
        }
    ) { innerPadding ->
        EditProfileContent(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            uiState = uiState,
            onNameChanged = editProfileViewModel::onNameChanged,
            onChangePhoto = {
                launcher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onSave = editProfileViewModel::saveProfile
        )
    }
}

@Composable
private fun EditProfileTopBar(onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                horizontal = Dimensions.SIZE_16,
                vertical = Dimensions.SIZE_12
            )
    ) {
        IconButton(
            onClick = onNavigateBack
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Text(
            modifier = Modifier.align(Alignment.Center),
            text = stringResource(R.string.title_edit_profile),
            style = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun EditProfileContent(
    modifier: Modifier = Modifier,
    uiState: EditProfileUiState,
    onNameChanged: (String) -> Unit,
    onChangePhoto: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val avatarModel = uiState.selectedAvatarUri ?: uiState.avatarUrl

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimensions.SIZE_16),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(Dimensions.SIZE_12))

        AsyncImage(
            modifier = Modifier
                .size(Dimensions.SIZE_80)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            model = ImageRequest.Builder(context)
                .data(avatarModel)
                .build(),
            placeholder = painterResource(R.drawable.img_profile_placeholder),
            error = painterResource(R.drawable.img_profile_placeholder),
            contentDescription = null
        )

        VerticalSpacing(Dimensions.SIZE_12)

        SecondaryButton(
            onClick = onChangePhoto,
            contentPadding = PaddingValues(
                horizontal = Dimensions.SIZE_24,
                vertical = Dimensions.SIZE_8
            )
        ) {
            Text(
                text = stringResource(R.string.label_change_photo),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        Spacer(modifier = Modifier.height(Dimensions.SIZE_24))

        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.name,
            onValueChange = onNameChanged,
            label = { Text(text = stringResource(R.string.label_name)) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        Spacer(modifier = Modifier.height(Dimensions.SIZE_16))

        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = uiState.email,
            onValueChange = {},
            enabled = false,
            label = { Text(text = stringResource(R.string.label_email)) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                disabledContainerColor = MaterialTheme.colorScheme.surface,
                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimensions.SIZE_16),
            onClick = onSave,
            enabled = uiState.isValid && !uiState.isSaving
        ) {
            Text(
                text = if (uiState.isSaving) {
                    stringResource(R.string.label_saving)
                } else {
                    stringResource(R.string.action_save_changes)
                },
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview
@Composable
private fun EditProfileScreenPreview() {
    CofinanceTheme {
        Surface {
            EditProfileContent(
                uiState = EditProfileUiState(
                    name = "Fawwaz",
                    email = "user@mail.com",
                    avatarUrl = ""
                ),
                onNameChanged = {},
                onChangePhoto = {},
                onSave = {}
            )
        }
    }
}
