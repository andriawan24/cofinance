package id.andriawan.cofinance.pages.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.content_description_profile_photo
import cofinance.composeapp.generated.resources.ic_calendar
import cofinance.composeapp.generated.resources.ic_chevron_right
import cofinance.composeapp.generated.resources.ic_edit
import cofinance.composeapp.generated.resources.ic_exit
import cofinance.composeapp.generated.resources.img_profile_placeholder
import cofinance.composeapp.generated.resources.label_cancel
import cofinance.composeapp.generated.resources.label_cycle_start_day
import cofinance.composeapp.generated.resources.label_cycle_start_day_description
import cofinance.composeapp.generated.resources.label_edit_profile
import cofinance.composeapp.generated.resources.label_logout
import cofinance.composeapp.generated.resources.label_logout_question
import cofinance.composeapp.generated.resources.label_profile
import cofinance.composeapp.generated.resources.label_profile_subtitle
import cofinance.composeapp.generated.resources.label_yes
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import id.andriawan.cofinance.components.ErrorBottomSheet
import id.andriawan.cofinance.components.PageTitle
import id.andriawan.cofinance.components.PrimaryButton
import id.andriawan.cofinance.components.SecondaryButton
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
    showMessage: (String) -> Unit,
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
        isUpdatingCycle = uiState.isUpdatingCycle,
        onSignedOut = { profileViewModel.toggleDialogLogout(true) },
        onEditProfile = onNavigateToEditProfile,
        onCycleStartDayChanged = { profileViewModel.updateCycleStartDay(it) }
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
    modifier: Modifier = Modifier,
    name: String,
    email: String,
    imageUrl: String,
    cycleStartDay: Int = 1,
    isUpdatingCycle: Boolean = false,
    onSignedOut: () -> Unit,
    onEditProfile: () -> Unit = {},
    onCycleStartDayChanged: (Int) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val displayName = remember(name, email) {
        name.ifBlank { email.substringBefore("@").ifBlank { email } }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(contentPadding)
                .padding(horizontal = Dimensions.SIZE_16, vertical = Dimensions.SIZE_24),
            verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_16)
        ) {
            PageTitle(title = stringResource(Res.string.label_profile))

            ProfileSummaryCard(
                name = displayName,
                email = email,
                imageUrl = imageUrl,
                onEditProfile = onEditProfile
            )

            CycleStartDaySetting(
                cycleStartDay = cycleStartDay,
                isUpdating = isUpdatingCycle,
                onDaySelected = onCycleStartDayChanged
            )

            SecondaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = Dimensions.SIZE_56),
                contentPadding = PaddingValues(
                    vertical = Dimensions.SIZE_16,
                    horizontal = Dimensions.SIZE_16
                ),
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
        }
    }
}

@Composable
private fun ProfileSummaryCard(
    name: String,
    email: String,
    imageUrl: String,
    onEditProfile: () -> Unit
) {
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
                Text(
                    text = stringResource(Res.string.label_profile_subtitle),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                    ),
                    modifier = Modifier.widthIn(max = Dimensions.SIZE_200)
                )

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

@Composable
private fun CycleStartDaySetting(
    cycleStartDay: Int,
    isUpdating: Boolean,
    onDaySelected: (Int) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .clickable(enabled = !isUpdating) { showPicker = true }
                .semantics { role = Role.Button }
                .padding(Dimensions.SIZE_16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12)
        ) {
            Box(
                modifier = Modifier
                    .size(Dimensions.SIZE_44)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(Dimensions.SIZE_20),
                    painter = painterResource(Res.drawable.ic_calendar),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.label_cycle_start_day),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = stringResource(Res.string.label_cycle_start_day_description),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(horizontal = Dimensions.SIZE_12, vertical = Dimensions.SIZE_8),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cycleStartDay.toString(),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            if (isUpdating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Dimensions.SIZE_20),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = Dimensions.SIZE_2
                )
            } else {
                Icon(
                    painter = painterResource(Res.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showPicker) {
        CycleStartDayPickerDialog(
            currentDay = cycleStartDay,
            onDaySelected = { day ->
                showPicker = false
                if (day != cycleStartDay) {
                    onDaySelected(day)
                }
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun CycleStartDayPickerDialog(
    currentDay: Int,
    onDaySelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDay by remember { mutableIntStateOf(currentDay) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.SIZE_24),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_20)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_4)) {
                    Text(
                        text = stringResource(Res.string.label_cycle_start_day),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = stringResource(Res.string.label_cycle_start_day_description),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                LazyVerticalGrid(
                    modifier = Modifier.fillMaxWidth(),
                    columns = GridCells.Adaptive(minSize = Dimensions.SIZE_44),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8),
                    userScrollEnabled = false
                ) {
                    items((1..28).toList()) { day ->
                        val isSelected = day == selectedDay

                        Box(
                            modifier = Modifier
                                .sizeIn(
                                    minWidth = Dimensions.SIZE_44,
                                    minHeight = Dimensions.SIZE_44
                                )
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerLow
                                    },
                                    shape = CircleShape
                                )
                                .clickable { selectedDay = day },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.toString(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            )
                        }
                    }
                }

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
                        onClick = onDismiss
                    ) {
                        Text(
                            text = stringResource(Res.string.label_cancel),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    PrimaryButton(
                        modifier = Modifier
                            .weight(1f)
                            .sizeIn(minHeight = Dimensions.SIZE_44),
                        onClick = { onDaySelected(selectedDay) },
                        horizontalPadding = Dimensions.SIZE_16,
                        verticalPadding = Dimensions.SIZE_12
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
                cycleStartDay = 25,
                isUpdatingCycle = false,
                onSignedOut = { }
            )
        }
    }
}
