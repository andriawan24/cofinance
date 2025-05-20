package id.andriawan24.cofinance.andro.ui.presentation.preview

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.components.PrimaryButton
import id.andriawan24.cofinance.andro.ui.components.VerticalSpacing
import id.andriawan24.cofinance.andro.ui.models.CofinanceAppState
import id.andriawan24.cofinance.andro.ui.models.rememberCofinanceAppState
import id.andriawan24.cofinance.andro.ui.navigation.Destinations
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.CollectAsEffect
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.ext.dropShadow
import org.koin.androidx.compose.koinViewModel

@Composable
fun PreviewScreen(
    appState: CofinanceAppState,
    imageUri: Uri,
    previewViewModel: PreviewViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val uiState by previewViewModel.previewUiState.collectAsStateWithLifecycle()

    previewViewModel.previewUiEvent.CollectAsEffect {
        when (it) {
            is PreviewUiEvent.NavigateToBalance -> {
                appState.navController.navigate(
                    Destinations.AddNew(
                        totalPrice = it.result.totalPrice,
                        date = it.result.transactionDate
                    )
                ) {
                    popUpTo<Destinations.AddNew> {
                        inclusive = true
                    }
                }
            }

            is PreviewUiEvent.ShowMessage -> appState.showSnackbar(it.message)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        IconButton(
            modifier = Modifier
                .padding(horizontal = Dimensions.SIZE_16)
                .padding(top = Dimensions.SIZE_24),
            onClick = appState.navController::navigateUp,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_left),
                contentDescription = null
            )
        }

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            VerticalSpacing(Dimensions.SIZE_24)

            AsyncImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black),
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUri)
                    .crossfade(200)
                    .build(),
                contentDescription = null
            )

            VerticalSpacing(Dimensions.SIZE_40)

            PrimaryButton(
                modifier = Modifier
                    .dropShadow(
                        shape = ButtonDefaults.shape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        blur = Dimensions.SIZE_10,
                        offsetY = Dimensions.SIZE_4
                    )
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.SIZE_16),
                onClick = {
                    previewViewModel.scanReceipt(
                        contentResolver = context.contentResolver,
                        imageUri = imageUri
                    )
                },
                enabled = !uiState.showLoading
            ) {
                Text(
                    text = stringResource(R.string.action_use_photo),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            VerticalSpacing(Dimensions.SIZE_16)

            TextButton(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                contentPadding = PaddingValues(
                    horizontal = Dimensions.SIZE_24,
                    vertical = Dimensions.SIZE_16
                ),
                onClick = appState.navController::navigateUp,
                enabled = !uiState.showLoading
            ) {
                Text(
                    text = stringResource(R.string.action_retake_photo),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewScreenPreview() {
    CofinanceTheme {
        Surface {
            PreviewScreen(
                appState = rememberCofinanceAppState(),
                imageUri = Uri.parse("https://picsum.photos/id/1/200/300")
            )
        }
    }
}