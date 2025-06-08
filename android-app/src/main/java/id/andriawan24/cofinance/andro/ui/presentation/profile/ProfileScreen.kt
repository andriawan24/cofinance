package id.andriawan24.cofinance.andro.ui.presentation.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.CollectAsEffect
import id.andriawan24.cofinance.andro.utils.Dimensions
import io.github.aakira.napier.Napier
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileScreen(
    onSignedOut: () -> Unit,
    profileViewModel: ProfileViewModel = koinViewModel()
) {
    // val user by profileViewModel.user.collectAsStateWithLifecycle()
    var showConfirmationLogoutDialog by remember { mutableStateOf(false) }

    profileViewModel.profileEvent.CollectAsEffect {
        when (it) {
            is ProfileEvent.NavigateToLoginPage -> onSignedOut()
            is ProfileEvent.ShowMessage -> Napier.e("Failed to logout ${it.message}")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimensions.SIZE_24),
                text = stringResource(R.string.title_profile),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = Dimensions.SIZE_24),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(Dimensions.SIZE_24))

                TextButton(
                    onClick = {
                        TODO("Not implemented yet")
                    }
                ) {
                    Text(
                        text = stringResource(R.string.label_edit_profile)
                    )
                }

                Spacer(modifier = Modifier.height(Dimensions.SIZE_12))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)
                ) {
                    Text(
                        modifier = Modifier.padding(horizontal = Dimensions.SIZE_24),
                        text = "Cash",
                        style = MaterialTheme.typography.labelMedium
                    )

                    Spacer(modifier = Modifier.height(Dimensions.SIZE_4))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.small
                            )
                            .clip(shape = MaterialTheme.shapes.small)
                            .padding(
                                vertical = Dimensions.SIZE_12,
                                horizontal = Dimensions.SIZE_24
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Blu by BCA",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        )

                        Icon(
                            imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                            contentDescription = null
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.small
                            )
                            .clip(shape = MaterialTheme.shapes.small)
                            .clickable(true) {
                                showConfirmationLogoutDialog = true
                            }
                            .padding(
                                vertical = Dimensions.SIZE_12,
                                horizontal = Dimensions.SIZE_24
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.label_logout),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }
    }

    if (showConfirmationLogoutDialog) {
        Dialog(
            onDismissRequest = {
                showConfirmationLogoutDialog = false
            },
        ) {
            Column(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(Dimensions.SIZE_24)
            ) {
                Text("Are you sure to logout?")

                Spacer(modifier = Modifier.height(Dimensions.SIZE_12))

                Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)) {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        onClick = {
                            showConfirmationLogoutDialog = false
                            profileViewModel.logout()
                        }
                    ) {
                        Text("Yes")
                    }

                    Button(
                        onClick = {
                            showConfirmationLogoutDialog = false
                        }
                    ) {
                        Text("Cancel")
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
            ProfileScreen(onSignedOut = { })
        }
    }
}