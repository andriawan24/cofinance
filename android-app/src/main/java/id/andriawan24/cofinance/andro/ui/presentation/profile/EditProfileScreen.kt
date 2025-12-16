package id.andriawan24.cofinance.andro.ui.presentation.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.components.PrimaryButton
import id.andriawan24.cofinance.andro.ui.components.SecondaryButton
import id.andriawan24.cofinance.andro.ui.components.VerticalSpacing
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.CollectAsEffect
import id.andriawan24.cofinance.andro.utils.Dimensions
import org.koin.androidx.compose.koinViewModel

@Composable
fun EditProfileScreen(
    onBackPressed: () -> Unit,
    onProfileUpdated: (String) -> Unit,
    editProfileViewModel: EditProfileViewModel = koinViewModel()
) {
    val uiState by editProfileViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val successMessage = stringResource(R.string.message_profile_update_success)
    val emptyNameMessage = stringResource(R.string.error_profile_empty_name)
    val genericError = stringResource(R.string.error_generic)

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                editProfileViewModel.onAvatarSelected(context.contentResolver, uri)
            }
        }
    )

    editProfileViewModel.events.CollectAsEffect { event ->
        when (event) {
            is EditProfileEvent.ShowMessage -> {
                val message = event.message.ifBlank { genericError }
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }

            EditProfileEvent.EmptyName -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(emptyNameMessage)
                }
            }

            EditProfileEvent.ProfileUpdated -> {
                onProfileUpdated(successMessage)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { contentPadding ->
        EditProfileContent(
            modifier = Modifier.padding(contentPadding),
            uiState = uiState,
            onBackPressed = onBackPressed,
            onNameChanged = editProfileViewModel::onNameChanged,
            onChangePhotoClicked = {
                imagePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onSaveClicked = editProfileViewModel::saveProfile
        )
    }

    if (uiState.isLoading) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                securePolicy = SecureFlagPolicy.SecureOn
            )
        ) {
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.onPrimary)
                    .padding(Dimensions.SIZE_24),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun EditProfileContent(
    modifier: Modifier = Modifier,
    uiState: EditProfileUiState,
    onBackPressed: () -> Unit,
    onNameChanged: (String) -> Unit,
    onChangePhotoClicked: () -> Unit,
    onSaveClicked: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimensions.SIZE_16)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimensions.SIZE_12)
                .padding(bottom = Dimensions.SIZE_24)
        ) {
            IconButton(
                onClick = onBackPressed,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_left),
                    contentDescription = null
                )
            }

            Text(
                modifier = Modifier.align(Alignment.Center),
                text = stringResource(R.string.title_edit_profile),
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.large
                )
                .padding(Dimensions.SIZE_24),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12)
        ) {
            AsyncImage(
                modifier = Modifier
                    .size(Dimensions.SIZE_80)
                    .clip(CircleShape)
                    .background(Color.White),
                model = ImageRequest.Builder(context)
                    .data(uiState.localAvatarUri ?: uiState.avatarUrl)
                    .build(),
                placeholder = painterResource(R.drawable.img_profile_placeholder),
                error = painterResource(R.drawable.img_profile_placeholder),
                contentDescription = stringResource(R.string.title_edit_profile)
            )

            SecondaryButton(
                contentPadding = PaddingValues(
                    horizontal = Dimensions.SIZE_16,
                    vertical = Dimensions.SIZE_8
                ),
                containerColor = MaterialTheme.colorScheme.onPrimary,
                contentColor = MaterialTheme.colorScheme.onBackground,
                onClick = onChangePhotoClicked
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = null
                    )

                    Text(
                        text = stringResource(R.string.action_change_photo),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        VerticalSpacing(Dimensions.SIZE_24)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.large
                )
                .padding(Dimensions.SIZE_16),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)
        ) {
            Text(
                text = stringResource(R.string.label_name),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            )

            BasicTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.name,
                onValueChange = onNameChanged,
                textStyle = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                decorationBox = { innerTextField ->
                    if (uiState.name.isBlank()) {
                        Text(
                            text = stringResource(R.string.hint_name_placeholder),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimensions.SIZE_24, bottom = Dimensions.SIZE_32),
            onClick = onSaveClicked,
            enabled = uiState.canSave && !uiState.isLoading
        ) {
            Text(
                text = stringResource(R.string.action_save_changes),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Preview
@Composable
private fun EditProfileScreenPreview() {
    CofinanceTheme {
        EditProfileContent(
            uiState = EditProfileUiState(name = "Fawwaz"),
            onBackPressed = { },
            onNameChanged = { },
            onChangePhotoClicked = { },
            onSaveClicked = { }
        )
    }
}
