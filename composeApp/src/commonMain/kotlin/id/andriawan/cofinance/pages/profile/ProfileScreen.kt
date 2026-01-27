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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.ic_exit
import cofinance.composeapp.generated.resources.img_profile_placeholder
import cofinance.composeapp.generated.resources.label_cancel
import cofinance.composeapp.generated.resources.label_logout
import cofinance.composeapp.generated.resources.label_logout_question
import cofinance.composeapp.generated.resources.label_profile
import cofinance.composeapp.generated.resources.label_yes
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import id.andriawan.cofinance.components.SecondaryButton
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.utils.extensions.CollectAsEffect
import id.andriawan24.cofinance.andro.ui.components.PageTitle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileScreen(
    onSignedOut: () -> Unit,
    showMessage: (String) -> Unit,
    profileViewModel: ProfileViewModel = koinViewModel()
) {
    val uiState by profileViewModel.uiState.collectAsStateWithLifecycle()

    profileViewModel.profileEvent.CollectAsEffect {
        when (it) {
            is ProfileEvent.NavigateToLoginPage -> onSignedOut()
            is ProfileEvent.ShowMessage -> showMessage(it.message)
        }
    }

    ProfileContent(
        name = profileViewModel.user.name,
        imageUrl = profileViewModel.user.avatarUrl,
        email = profileViewModel.user.email,
        onSignedOut = { profileViewModel.toggleDialogLogout(true) }
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
}

@Composable
fun ProfileContent(
    modifier: Modifier = Modifier,
    name: String,
    email: String,
    imageUrl: String,
    onSignedOut: () -> Unit
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

//                SecondaryButton(
//                    contentPadding = PaddingValues(
//                        vertical = Dimensions.SIZE_8,
//                        horizontal = Dimensions.SIZE_16
//                    ),
//                    onClick = {}
//                ) {
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_4)
//                    ) {
//                        Image(
//                            modifier = Modifier.size(Dimensions.SIZE_24),
//                            painter = painterResource(R.drawable.ic_edit),
//                            contentDescription = null
//                        )
//
//                        Text(
//                            text = "Edit Profile",
//                            style = MaterialTheme.typography.labelSmall.copy(
//                                color = MaterialTheme.colorScheme.onSurface
//                            )
//                        )
//                    }
//                }
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
