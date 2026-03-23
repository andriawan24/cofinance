package id.andriawan.cofinance.pages.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.ic_calendar
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
import cofinance.composeapp.generated.resources.label_yes
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import id.andriawan.cofinance.components.ErrorBottomSheet
import id.andriawan.cofinance.components.SecondaryButton
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.UiText
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.utils.extensions.CollectAsEffect
import id.andriawan.cofinance.components.PageTitle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
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
        onSignedOut = { profileViewModel.toggleDialogLogout(true) },
        onEditProfile = onNavigateToEditProfile,
        onCycleStartDayChanged = { profileViewModel.updateCycleStartDay(it) }
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
    onSignedOut: () -> Unit,
    onEditProfile: () -> Unit = {},
    onCycleStartDayChanged: (Int) -> Unit = {}
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

        // Cycle start day setting
        CycleStartDaySetting(
            modifier = Modifier.padding(Dimensions.SIZE_16),
            cycleStartDay = cycleStartDay,
            onDaySelected = onCycleStartDayChanged
        )

        SecondaryButton(
            modifier = Modifier.padding(horizontal = Dimensions.SIZE_16),
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

@Composable
private fun CycleStartDaySetting(
    modifier: Modifier = Modifier,
    cycleStartDay: Int,
    onDaySelected: (Int) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            )
            .clickable { showPicker = true }
            .padding(Dimensions.SIZE_16),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12)
    ) {
        Box(
            modifier = Modifier
                .size(Dimensions.SIZE_40)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
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
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = Modifier.height(Dimensions.SIZE_2))

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
                .padding(horizontal = Dimensions.SIZE_12, vertical = Dimensions.SIZE_6),
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large
                )
                .padding(Dimensions.SIZE_24)
        ) {
            Text(
                text = stringResource(Res.string.label_cycle_start_day),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(modifier = Modifier.height(Dimensions.SIZE_4))

            Text(
                text = stringResource(Res.string.label_cycle_start_day_description),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(Dimensions.SIZE_20))

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_6),
                verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_6)
            ) {
                items((1..28).toList()) { day ->
                    val isSelected = day == selectedDay

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .then(
                                if (isSelected) {
                                    Modifier.background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                } else {
                                    Modifier.background(
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        shape = CircleShape
                                    )
                                }
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

            Spacer(modifier = Modifier.height(Dimensions.SIZE_20))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = Dimensions.zero
                    ),
                    onClick = onDismiss
                ) {
                    Text(
                        text = stringResource(Res.string.label_cancel),
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Button(
                    onClick = { onDaySelected(selectedDay) }
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
                onSignedOut = { }
            )
        }
    }
}
