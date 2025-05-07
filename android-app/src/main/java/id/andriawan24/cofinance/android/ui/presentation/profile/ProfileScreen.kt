package id.andriawan24.cofinance.android.ui.presentation.profile

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import id.andriawan24.cofinance.android.R
import id.andriawan24.cofinance.android.ui.models.CofinanceAppState
import id.andriawan24.cofinance.android.ui.models.rememberCofinanceAppState
import id.andriawan24.cofinance.android.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.android.utils.Dimensions
import id.andriawan24.cofinance.android.utils.TextSizes

@Composable
fun ProfileScreen(appState: CofinanceAppState, onSignedOut: () -> Unit) {
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
                AsyncImage(
                    modifier = Modifier
                        .size(Dimensions.SIZE_80)
                        .clip(CircleShape),
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(appState.user?.photoUrl)
                        .placeholder(R.drawable.img_placeholder_profile)
                        .error(R.drawable.img_placeholder_profile)
                        .crossfade(true)
                        .build(),
                    contentDescription = null
                )

                Spacer(modifier = Modifier.height(Dimensions.SIZE_24))

                Text(
                    text = appState.user?.displayName.orEmpty(),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = TextSizes.SIZE_20
                    )
                )

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
                                appState.signOut(onSignedOut)
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
            ProfileScreen(appState = rememberCofinanceAppState(), onSignedOut = { })
        }
    }
}