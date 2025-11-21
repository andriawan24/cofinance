package id.andriawan24.cofinance.andro.ui.presentation.profile

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.components.SecondaryButton
import id.andriawan24.cofinance.andro.ui.components.VerticalSpacing
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.CollectAsEffect
import id.andriawan24.cofinance.andro.utils.Dimensions
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileScreen(
    onSignedOut: () -> Unit,
    showMessage: (String) -> Unit,
    profileViewModel: ProfileViewModel = koinViewModel()
) {
    val context = LocalContext.current
    var showConfirmationLogoutDialog by remember { mutableStateOf(false) }

    profileViewModel.profileEvent.CollectAsEffect {
        when (it) {
            is ProfileEvent.NavigateToLoginPage -> onSignedOut()
            is ProfileEvent.ShowMessage -> showMessage(context.getString(it.messageResId))
        }
    }

    ProfileContent(
        name = profileViewModel.user.name,
        imageUrl = profileViewModel.user.avatarUrl,
        email = profileViewModel.user.email,
        onSignedOut = {
            showConfirmationLogoutDialog = true
        }
    )

    if (showConfirmationLogoutDialog) {
        Dialog(onDismissRequest = { }) {
            Column(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(Dimensions.SIZE_24)
            ) {
                Text(stringResource(R.string.label_logout_question))
                Spacer(modifier = Modifier.height(Dimensions.SIZE_12))
                Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)) {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        onClick = {
                            profileViewModel.logout()
                        }
                    ) {
                        Text("Yes")
                    }

                    Button(
                        onClick = {}
                    ) {
                        Text(stringResource(R.string.label_cancel))
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
        Title(
            modifier = Modifier.padding(
                horizontal = Dimensions.SIZE_16,
                vertical = Dimensions.SIZE_24
            )
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
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUrl)
                        .build(),
                    placeholder = painterResource(R.drawable.img_profile_placeholder),
                    error = painterResource(R.drawable.img_profile_placeholder),
                    contentDescription = null
                )

                VerticalSpacing(Dimensions.SIZE_12)

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = name,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White
                    )
                )

                VerticalSpacing(Dimensions.SIZE_4)

                Text(
                    text = email,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                )

                VerticalSpacing(Dimensions.SIZE_12)

                SecondaryButton(
                    contentPadding = PaddingValues(
                        vertical = Dimensions.SIZE_8,
                        horizontal = Dimensions.SIZE_16
                    ),
                    onClick = {}
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_4)
                    ) {
                        Image(
                            modifier = Modifier.size(Dimensions.SIZE_24),
                            painter = painterResource(R.drawable.ic_edit),
                            contentDescription = null
                        )

                        Text(
                            text = "Edit Profile",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
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
                    painter = painterResource(R.drawable.ic_exit),
                    contentDescription = null
                )

                Text(
                    text = stringResource(R.string.label_logout),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
private fun Title(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(R.string.title_profile),
            style = MaterialTheme.typography.displaySmall
        )
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
                onSignedOut = {

                }
            )
        }
    }
}