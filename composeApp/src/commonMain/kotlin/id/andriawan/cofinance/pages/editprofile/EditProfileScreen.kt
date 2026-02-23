package id.andriawan.cofinance.pages.editprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.action_save
import cofinance.composeapp.generated.resources.ic_arrow_left
import cofinance.composeapp.generated.resources.ic_camera
import cofinance.composeapp.generated.resources.img_profile_placeholder
import cofinance.composeapp.generated.resources.label_edit_profile
import cofinance.composeapp.generated.resources.label_name
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import id.andriawan.cofinance.components.PrimaryButton
import id.andriawan.cofinance.components.rememberGalleryLauncher
import id.andriawan.cofinance.utils.Dimensions
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
    showMessage: (String) -> Unit,
    editProfileViewModel: EditProfileViewModel = koinViewModel()
) {
    val uiState by editProfileViewModel.uiState.collectAsStateWithLifecycle()
    val snackState = remember { SnackbarHostState() }
    val context = LocalPlatformContext.current

    val openGallery = rememberGalleryLauncher { uri ->
        val bytes = readFromFile(context, uri)
        if (bytes != null) {
            val compressed = compressImage(bytes)
            editProfileViewModel.onImageSelected(uri, compressed)
        }
    }

    editProfileViewModel.event.CollectAsEffect {
        when (it) {
            is EditProfileEvent.ProfileUpdated -> onProfileUpdated()
            is EditProfileEvent.ShowError -> showMessage(it.message)
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackState) }) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            // Top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimensions.SIZE_12, bottom = Dimensions.SIZE_24)
                    .padding(horizontal = Dimensions.SIZE_16)
            ) {
                IconButton(
                    onClick = onBackClicked,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_left),
                        contentDescription = null
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

            Spacer(modifier = Modifier.height(Dimensions.SIZE_20))

            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(Dimensions.SIZE_120)
                    .clickable { openGallery() },
                contentAlignment = Alignment.BottomEnd
            ) {
                val imageModel = if (uiState.localImageUri != null) {
                    ImageRequest.Builder(context)
                        .data(uiState.localImageUri)
                        .build()
                } else {
                    ImageRequest.Builder(context)
                        .data(uiState.avatarUrl)
                        .build()
                }

                AsyncImage(
                    modifier = Modifier
                        .size(Dimensions.SIZE_120)
                        .clip(CircleShape)
                        .background(Color.White),
                    model = imageModel,
                    placeholder = painterResource(Res.drawable.img_profile_placeholder),
                    error = painterResource(Res.drawable.img_profile_placeholder),
                    contentScale = ContentScale.Crop,
                    contentDescription = null
                )

                Box(
                    modifier = Modifier
                        .size(Dimensions.SIZE_36)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.size(Dimensions.SIZE_20),
                        painter = painterResource(Res.drawable.ic_camera),
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.SIZE_24))

            // Name field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.SIZE_16)
                    .background(
                        color = MaterialTheme.colorScheme.onPrimary,
                        shape = MaterialTheme.shapes.large
                    )
                    .border(
                        width = Dimensions.SIZE_2,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.large
                    )
                    .padding(all = Dimensions.SIZE_16),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_10),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    modifier = Modifier.weight(1f),
                    value = uiState.name,
                    onValueChange = { editProfileViewModel.onNameChanged(it) },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Text
                    ),
                    textStyle = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    decorationBox = { innerTextField ->
                        if (uiState.name.isBlank()) {
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
                )
            }

            Spacer(Modifier.weight(1f))

            // Save button
            PrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = Dimensions.SIZE_24),
                onClick = { editProfileViewModel.saveProfile() },
                enabled = !uiState.isLoading && uiState.name.isNotBlank()
            ) {
                Text(
                    text = stringResource(Res.string.action_save),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
