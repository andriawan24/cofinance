package id.andriawan.cofinance.pages.profile

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.action_delete_model
import cofinance.composeapp.generated.resources.action_download_model
import cofinance.composeapp.generated.resources.ic_edit
import cofinance.composeapp.generated.resources.ic_exit
import cofinance.composeapp.generated.resources.img_profile_placeholder
import cofinance.composeapp.generated.resources.label_ai_model
import cofinance.composeapp.generated.resources.label_ai_model_description
import cofinance.composeapp.generated.resources.label_ai_model_not_downloaded
import cofinance.composeapp.generated.resources.label_ai_model_ready
import cofinance.composeapp.generated.resources.label_ai_model_size
import cofinance.composeapp.generated.resources.label_cancel
import cofinance.composeapp.generated.resources.label_delete_model_question
import cofinance.composeapp.generated.resources.label_downloading_model
import cofinance.composeapp.generated.resources.label_edit_profile
import cofinance.composeapp.generated.resources.label_logout
import cofinance.composeapp.generated.resources.label_logout_question
import cofinance.composeapp.generated.resources.label_profile
import cofinance.composeapp.generated.resources.label_yes
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import id.andriawan.cofinance.components.ErrorBottomSheet
import id.andriawan.cofinance.components.PageTitle
import id.andriawan.cofinance.components.SecondaryButton
import id.andriawan.cofinance.data.datasource.ModelStatus
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.utils.UiText
import id.andriawan.cofinance.utils.extensions.CollectAsEffect
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(
    onSignedOut: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    profileViewModel: ProfileViewModel = koinViewModel()
) {
    val uiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val user by profileViewModel.user.collectAsStateWithLifecycle()
    val modelStatus by profileViewModel.modelStatus.collectAsStateWithLifecycle()
    var errorUiText by remember { mutableStateOf<UiText?>(null) }

    profileViewModel.profileEvent.CollectAsEffect {
        when (it) {
            is ProfileEvent.NavigateToLoginPage -> onSignedOut()
            is ProfileEvent.ShowMessage -> errorUiText = it.message
        }
    }

    ProfileContent(
        name = user.name,
        imageUrl = user.avatarUrl,
        email = user.email,
        modelStatus = modelStatus,
        onSignedOut = { profileViewModel.toggleDialogLogout(true) },
        onEditProfile = onNavigateToEditProfile,
        onDownloadModel = { profileViewModel.downloadModel() },
        onDeleteModel = { profileViewModel.toggleDeleteModelDialog(true) }
    )

    if (uiState.isShowDialogLogout) {
        Dialog(onDismissRequest = { profileViewModel.toggleDialogLogout(false) }) {
            Column(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(Dimensions.SIZE_24)
            ) {
                Text(stringResource(Res.string.label_logout_question))

                Spacer(modifier = Modifier.height(Dimensions.SIZE_12))

                Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)) {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        onClick = {
                            profileViewModel.logout()
                            profileViewModel.toggleDialogLogout(false)
                        }
                    ) {
                        Text(stringResource(Res.string.label_yes))
                    }

                    Button(onClick = { profileViewModel.toggleDialogLogout(false) }) {
                        Text(stringResource(Res.string.label_cancel))
                    }
                }
            }
        }
    }

    if (uiState.isShowDeleteModelDialog) {
        Dialog(onDismissRequest = { profileViewModel.toggleDeleteModelDialog(false) }) {
            Column(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(Dimensions.SIZE_24)
            ) {
                Text(stringResource(Res.string.label_delete_model_question))

                Spacer(modifier = Modifier.height(Dimensions.SIZE_12))

                Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)) {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        onClick = {
                            // TODO: implement delete when needed
                            profileViewModel.toggleDeleteModelDialog(false)
                        }
                    ) {
                        Text(stringResource(Res.string.label_yes))
                    }

                    Button(onClick = { profileViewModel.toggleDeleteModelDialog(false) }) {
                        Text(stringResource(Res.string.label_cancel))
                    }
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
fun ProfileContent(
    modifier: Modifier = Modifier,
    name: String,
    email: String,
    imageUrl: String,
    modelStatus: ModelStatus = ModelStatus.NotDownloaded,
    onSignedOut: () -> Unit,
    onEditProfile: () -> Unit = {},
    onDownloadModel: () -> Unit = {},
    onDeleteModel: () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxSize()) {
        PageTitle(
            modifier = Modifier.padding(Dimensions.SIZE_16, Dimensions.SIZE_24),
            title = stringResource(Res.string.label_profile)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SIZE_16)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.large
                )
        ) {
            Column(modifier = Modifier.padding(Dimensions.SIZE_16)) {
                AsyncImage(
                    modifier = Modifier
                        .size(Dimensions.SIZE_45)
                        .clip(CircleShape)
                        .background(Color.White),
                    model = ImageRequest.Builder(LocalPlatformContext.current)
                        .data(imageUrl)
                        .build(),
                    placeholder = painterResource(Res.drawable.img_profile_placeholder),
                    error = painterResource(Res.drawable.img_profile_placeholder),
                    contentScale = ContentScale.Crop,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.height(Dimensions.SIZE_12))

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = name,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(Dimensions.SIZE_4))

                Text(
                    text = email,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = Modifier.height(Dimensions.SIZE_12))

                SecondaryButton(
                    contentPadding = PaddingValues(
                        vertical = Dimensions.SIZE_8,
                        horizontal = Dimensions.SIZE_16
                    ),
                    onClick = onEditProfile
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_4)
                    ) {
                        Image(
                            modifier = Modifier.size(Dimensions.SIZE_24),
                            painter = painterResource(Res.drawable.ic_edit),
                            contentDescription = null
                        )

                        Text(
                            text = stringResource(Res.string.label_edit_profile),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        }

        // AI Model Manager Section
        Spacer(modifier = Modifier.height(Dimensions.SIZE_16))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SIZE_16)
                .background(
                    color = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.large
                )
        ) {
            Column(modifier = Modifier.padding(Dimensions.SIZE_16)) {
                Text(
                    text = stringResource(Res.string.label_ai_model),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(Dimensions.SIZE_4))

                Text(
                    text = stringResource(Res.string.label_ai_model_description),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(Dimensions.SIZE_12))

                when (modelStatus) {
                    is ModelStatus.Ready -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.label_ai_model_ready),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )

                            SecondaryButton(
                                contentPadding = PaddingValues(
                                    vertical = Dimensions.SIZE_8,
                                    horizontal = Dimensions.SIZE_16
                                ),
                                onClick = onDeleteModel
                            ) {
                                Text(
                                    text = stringResource(Res.string.action_delete_model),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.error
                                    )
                                )
                            }
                        }
                    }

                    is ModelStatus.Downloading -> {
                        val progress = modelStatus.progress
                        Text(
                            text = stringResource(
                                Res.string.label_downloading_model,
                                progress.times(100).toInt()
                            ),
                            style = MaterialTheme.typography.labelMedium
                        )

                        Spacer(modifier = Modifier.height(Dimensions.SIZE_8))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth(),
                            strokeCap = StrokeCap.Round
                        )
                    }

                    is ModelStatus.NotDownloaded, is ModelStatus.Error -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = stringResource(Res.string.label_ai_model_not_downloaded),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Text(
                                    text = stringResource(Res.string.label_ai_model_size),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }

                            Button(onClick = onDownloadModel) {
                                Text(
                                    text = stringResource(Res.string.action_download_model),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    is ModelStatus.LoadingModel, is ModelStatus.Inferring -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(Dimensions.SIZE_16),
                                strokeWidth = Dimensions.SIZE_2
                            )
                            Text(
                                text = stringResource(Res.string.label_ai_model_ready),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }

        SecondaryButton(
            modifier = Modifier.padding(Dimensions.SIZE_16),
            contentPadding = PaddingValues(
                vertical = Dimensions.SIZE_16,
                horizontal = Dimensions.SIZE_16
            ),
            containerColor = MaterialTheme.colorScheme.onPrimary,
            contentColor = MaterialTheme.colorScheme.onBackground,
            shape = MaterialTheme.shapes.large,
            onClick = onSignedOut
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_exit),
                    contentDescription = null
                )

                Text(
                    text = stringResource(Res.string.label_logout),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Preview
@Composable
private fun ProfileScreenPreview() {
    CofinanceTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            ProfileContent(
                imageUrl = "https://someimage.com",
                name = "Fawwaz",
                email = "andriawan2422@gmail.com",
                onSignedOut = { }
            )
        }
    }
}
