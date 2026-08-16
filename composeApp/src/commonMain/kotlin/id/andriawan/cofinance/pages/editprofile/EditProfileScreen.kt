package id.andriawan.cofinance.pages.editprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.action_back
import cofinance.composeapp.generated.resources.action_change_photo
import cofinance.composeapp.generated.resources.action_save
import cofinance.composeapp.generated.resources.content_description_change_profile_photo
import cofinance.composeapp.generated.resources.content_description_profile_photo
import cofinance.composeapp.generated.resources.ic_arrow_left
import cofinance.composeapp.generated.resources.ic_camera
import cofinance.composeapp.generated.resources.img_profile_placeholder
import cofinance.composeapp.generated.resources.label_edit_profile
import cofinance.composeapp.generated.resources.label_name
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import id.andriawan.cofinance.components.ErrorBottomSheet
import id.andriawan.cofinance.components.PrimaryButton
import id.andriawan.cofinance.components.SecondaryButton
import id.andriawan.cofinance.components.rememberGalleryLauncher
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.utils.UiText
import id.andriawan.cofinance.utils.compressImage
import id.andriawan.cofinance.utils.extensions.CollectAsEffect
import id.andriawan.cofinance.utils.readFromFile
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EditProfileScreen(
    onBackClicked: () -> Unit,
    onProfileUpdated: () -> Unit,
    editProfileViewModel: EditProfileViewModel = koinViewModel()
) {
    val uiState by editProfileViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalPlatformContext.current
    var errorUiText by remember { mutableStateOf<UiText?>(null) }
    val scrollState = rememberScrollState()

    val openGalleryLauncher = rememberGalleryLauncher { uri ->
        val bytes = readFromFile(context, uri)
        if (bytes != null) {
            val compressed = compressImage(bytes)
            editProfileViewModel.onImageSelected(uri, compressed)
        }
    }

    editProfileViewModel.event.CollectAsEffect {
        when (it) {
            is EditProfileEvent.ProfileUpdated -> onProfileUpdated()
            is EditProfileEvent.ShowError -> errorUiText = it.message
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = Dimensions.SIZE_16, vertical = Dimensions.SIZE_24),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
        ) {
            EditProfileTitle(onBackClicked = onBackClicked)
            EditProfilePicture(
                localImageUri = uiState.localImageUri,
                avatarUrl = uiState.avatarUrl,
                isLoading = uiState.isLoading,
                onOpenGalleryClicked = openGalleryLauncher
            )
            EditProfileName(
                name = uiState.name,
                isLoading = uiState.isLoading,
                onNameChanged = editProfileViewModel::onNameChanged
            )
            PrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = Dimensions.SIZE_56),
                onClick = { editProfileViewModel.saveProfile() },
                enabled = !uiState.isLoading && uiState.name.isNotBlank()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(Dimensions.SIZE_20),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = Dimensions.SIZE_2
                        )
                    }

                    Text(
                        text = stringResource(Res.string.action_save),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }

    ErrorBottomSheet(
        message = errorUiText?.asString(),
        onDismiss = { errorUiText = null }
    )
}

@Composable
private fun EditProfilePicture(
    localImageUri: String?,
    avatarUrl: String,
    isLoading: Boolean,
    onOpenGalleryClicked: () -> Unit
) {
    val context = LocalPlatformContext.current
    val imageModel = if (localImageUri != null) {
        ImageRequest.Builder(context)
            .data(localImageUri)
            .build()
    } else {
        ImageRequest.Builder(context)
            .data(avatarUrl)
            .build()
    }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = Dimensions.SIZE_20, y = -Dimensions.SIZE_24)
                    .size(Dimensions.SIZE_96)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f))
            )

            Column(
                modifier = Modifier.fillMaxWidth().padding(Dimensions.SIZE_20),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
            ) {
                Box(
                    modifier = Modifier
                        .size(Dimensions.SIZE_120)
                        .clickable(enabled = !isLoading, onClick = onOpenGalleryClicked),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    AsyncImage(
                        modifier = Modifier
                            .size(Dimensions.SIZE_120)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        model = imageModel,
                        placeholder = painterResource(Res.drawable.img_profile_placeholder),
                        error = painterResource(Res.drawable.img_profile_placeholder),
                        contentScale = ContentScale.Crop,
                        contentDescription = stringResource(Res.string.content_description_profile_photo)
                    )

                    Box(
                        modifier = Modifier
                            .size(Dimensions.SIZE_40)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier.size(Dimensions.SIZE_20),
                            painter = painterResource(Res.drawable.ic_camera),
                            contentDescription = stringResource(Res.string.content_description_change_profile_photo),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                SecondaryButton(
                    modifier = Modifier.sizeIn(minHeight = Dimensions.SIZE_44),
                    onClick = onOpenGalleryClicked,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    contentPadding = PaddingValues(
                        horizontal = Dimensions.SIZE_16,
                        vertical = Dimensions.SIZE_10
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_camera),
                            contentDescription = null
                        )

                        Text(
                            text = stringResource(Res.string.action_change_photo),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditProfileTitle(onBackClicked: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        IconButton(
            modifier = Modifier.size(Dimensions.SIZE_44),
            onClick = onBackClicked,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_arrow_left),
                contentDescription = stringResource(Res.string.action_back)
            )
        }

        Text(
            modifier = Modifier.align(Alignment.Center),
            text = stringResource(Res.string.label_edit_profile),
            style = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onBackground
            )
        )
    }
}

@Composable
private fun EditProfileName(
    name: String,
    isLoading: Boolean,
    onNameChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)) {
        Text(
            text = stringResource(Res.string.label_name),
            style = MaterialTheme.typography.titleSmall.copy(
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large
                )
                .border(
                    width = Dimensions.SIZE_1,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = MaterialTheme.shapes.large
                )
                .padding(all = Dimensions.SIZE_16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                modifier = Modifier.fillMaxWidth(),
                value = name,
                onValueChange = onNameChanged,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text
                ),
                enabled = !isLoading,
                textStyle = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (name.isBlank()) {
                            Text(
                                text = stringResource(Res.string.label_name),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        innerTextField()
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditProfileTitlePreview() {
    CofinanceTheme {
        EditProfileTitle {

        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditProfilePicturePreview() {
    CofinanceTheme {
        EditProfilePicture(
            localImageUri = null,
            avatarUrl = "https://picsum.photos/200/300",
            isLoading = false,
            onOpenGalleryClicked = { }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditProfileNamePreview() {
    CofinanceTheme {
        EditProfileName(
            name = "Fawwaz",
            isLoading = false,
            onNameChanged = {}
        )
    }
}
