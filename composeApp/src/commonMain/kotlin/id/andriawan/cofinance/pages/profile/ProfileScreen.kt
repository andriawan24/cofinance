package id.andriawan.cofinance.pages.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.action_sign_in_to_sync
import cofinance.composeapp.generated.resources.content_description_profile_photo
import cofinance.composeapp.generated.resources.description_local_only_mode
import cofinance.composeapp.generated.resources.ic_edit
import cofinance.composeapp.generated.resources.ic_exit
import cofinance.composeapp.generated.resources.img_profile_placeholder
import cofinance.composeapp.generated.resources.label_cancel
import cofinance.composeapp.generated.resources.label_edit_profile
import cofinance.composeapp.generated.resources.label_logout
import cofinance.composeapp.generated.resources.label_logout_question
import cofinance.composeapp.generated.resources.label_profile
import cofinance.composeapp.generated.resources.label_profile_subtitle
import cofinance.composeapp.generated.resources.label_yes
import cofinance.composeapp.generated.resources.title_local_only_mode
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import id.andriawan.cofinance.components.ErrorBottomSheet
import id.andriawan.cofinance.components.PageTitle
import id.andriawan.cofinance.components.PrimaryButton
import id.andriawan.cofinance.components.SecondaryButton
import id.andriawan.cofinance.pages.profile.components.CycleStartDaySetting
import id.andriawan.cofinance.pages.profile.security.SecuritySettingsSection
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
    onSignIn: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    profileViewModel: ProfileViewModel = koinViewModel()
) {
    val uiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val user by profileViewModel.user.collectAsStateWithLifecycle()
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
        cycleStartDay = user.cycleStartDay,
        isSignedIn = uiState.isSignedIn,
        isUpdatingCycle = uiState.isUpdatingCycle,
        onSignIn = onSignIn,
        onSignedOut = { profileViewModel.toggleDialogLogout(true) },
        onEditProfile = onNavigateToEditProfile,
        onCycleStartDayChanged = { profileViewModel.updateCycleStartDay(it) },
        securitySettings = { SecuritySettingsSection() }
    )

    if (uiState.isShowDialogLogout) {
        Dialog(onDismissRequest = { profileViewModel.toggleDialogLogout(false) }) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(Dimensions.SIZE_24),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_20)
                ) {
                    Text(
                        text = stringResource(Res.string.label_logout_question),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12)
                    ) {
                        SecondaryButton(
                            modifier = Modifier
                                .weight(1f)
                                .sizeIn(minHeight = Dimensions.SIZE_44),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            onClick = { profileViewModel.toggleDialogLogout(false) }
                        ) {
                            Text(
                                text = stringResource(Res.string.label_cancel),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .sizeIn(minHeight = Dimensions.SIZE_44),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            onClick = {
                                profileViewModel.logout()
                                profileViewModel.toggleDialogLogout(false)
                            }
                        ) {
                            Text(
                                text = stringResource(Res.string.label_yes),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
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
    name: String,
    email: String,
    imageUrl: String,
    cycleStartDay: Int = 1,
    isSignedIn: Boolean = true,
    isUpdatingCycle: Boolean = false,
    onSignIn: () -> Unit = {},
    onSignedOut: () -> Unit,
    onEditProfile: () -> Unit = {},
    onCycleStartDayChanged: (Int) -> Unit = {},
    /**
     * The lock controls, passed in rather than composed here so that this function stays previewable
     * without a dependency graph. [ProfileScreen] supplies the real section.
     */
    securitySettings: @Composable () -> Unit = {}
) {
    val displayName = remember(name, email) {
        name.ifBlank { email.substringBefore("@").ifBlank { email } }
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .padding(
                horizontal = Dimensions.SIZE_16,
                vertical = Dimensions.SIZE_24
            ),
        verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
    ) {
        PageTitle(
            modifier = Modifier,
            title = stringResource(Res.string.label_profile)
        )

        if (isSignedIn) {
            ProfileSummaryCard(
                modifier = Modifier,
                name = displayName,
                email = email,
                imageUrl = imageUrl,
                onEditProfile = onEditProfile
            )

            CycleStartDaySetting(
                modifier = Modifier,
                cycleStartDay = cycleStartDay,
                isUpdating = isUpdatingCycle,
                onDaySelected = onCycleStartDayChanged
            )

            // Renders nothing until encryption setup has completed, so a signed-in user who has not
            // finished setup — and every local-only user, who never reaches this branch at all —
            // sees no lock controls.
            securitySettings()

            SecondaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = Dimensions.SIZE_56),
                contentPadding = PaddingValues(Dimensions.SIZE_16),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.error,
                shape = MaterialTheme.shapes.large,
                onClick = onSignedOut
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_exit),
                        contentDescription = null
                    )

                    Text(
                        text = stringResource(Res.string.label_logout),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        } else {
            LocalOnlyProfileCard(
                modifier = Modifier.padding(Dimensions.SIZE_16),
                onSignIn = onSignIn
            )
        }
    }
}

@Composable
private fun LocalOnlyProfileCard(
    modifier: Modifier = Modifier,
    onSignIn: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.SIZE_24),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
        ) {
            Text(
                text = stringResource(Res.string.title_local_only_mode),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = stringResource(Res.string.description_local_only_mode),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            PrimaryButton(
                modifier = Modifier.fillMaxWidth().sizeIn(minHeight = Dimensions.SIZE_52),
                onClick = onSignIn
            ) {
                Text(text = stringResource(Res.string.action_sign_in_to_sync))
            }
        }
    }
}

@Composable
private fun ProfileSummaryCard(
    modifier: Modifier = Modifier,
    name: String,
    email: String,
    imageUrl: String,
    onEditProfile: () -> Unit
) {
    Surface(
        modifier = modifier,
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

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = -Dimensions.SIZE_24, y = Dimensions.SIZE_28)
                    .size(Dimensions.SIZE_64)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.06f))
            )

            Column(
                modifier = Modifier.padding(Dimensions.SIZE_20),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        modifier = Modifier
                            .size(Dimensions.SIZE_72)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface),
                        model = ImageRequest.Builder(LocalPlatformContext.current)
                            .data(imageUrl)
                            .build(),
                        placeholder = painterResource(Res.drawable.img_profile_placeholder),
                        error = painterResource(Res.drawable.img_profile_placeholder),
                        contentScale = ContentScale.Crop,
                        contentDescription = stringResource(Res.string.content_description_profile_photo)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_4)
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )

                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        )
                    }
                }

                SecondaryButton(
                    modifier = Modifier.sizeIn(minHeight = Dimensions.SIZE_44),
                    contentPadding = PaddingValues(
                        vertical = Dimensions.SIZE_10,
                        horizontal = Dimensions.SIZE_16
                    ),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = onEditProfile
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)
                    ) {
                        Icon(
                            modifier = Modifier.size(Dimensions.SIZE_20),
                            painter = painterResource(Res.drawable.ic_edit),
                            contentDescription = null
                        )

                        Text(
                            text = stringResource(Res.string.label_edit_profile),
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

@Preview
@Composable
private fun LoggedInUserPreview() {
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
                cycleStartDay = 25,
                isUpdatingCycle = false,
                onSignedOut = { }
            )
        }
    }
}

@Preview
@Composable
private fun NotLoggedInUserPreview() {
    CofinanceTheme {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            ProfileContent(
                isSignedIn = false,
                name = "",
                email = "",
                imageUrl = "",
                onSignedOut = { },
            )
        }
    }
}
