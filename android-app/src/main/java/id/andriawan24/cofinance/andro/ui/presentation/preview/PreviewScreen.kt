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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import id.andriawan24.cofinance.andro.R
import id.andriawan24.cofinance.andro.ui.components.PrimaryButton
import id.andriawan24.cofinance.andro.ui.components.VerticalSpacing
import id.andriawan24.cofinance.andro.ui.theme.CofinanceTheme
import id.andriawan24.cofinance.andro.utils.CollectAsEffect
import id.andriawan24.cofinance.andro.utils.Dimensions
import id.andriawan24.cofinance.andro.utils.ext.dropShadow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun PreviewScreen(
    imageUri: Uri,
    onNavigateToAdd: (String) -> Unit,
    onNavigateBack: () -> Unit,
    previewViewModel: PreviewViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackState = remember { SnackbarHostState() }
    val uiState by previewViewModel.previewUiState.collectAsStateWithLifecycle()

    previewViewModel.previewUiEvent.CollectAsEffect {
        when (it) {
            is PreviewUiEvent.NavigateToBalance -> onNavigateToAdd(it.transactionId)
            is PreviewUiEvent.ShowMessage -> scope.launch {
                snackState.showSnackbar(it.message)
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackState) }) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize()
        ) {
            IconButton(
                modifier = Modifier
                    .padding(horizontal = Dimensions.SIZE_16)
                    .padding(top = Dimensions.SIZE_24),
                onClick = onNavigateBack,
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
                    onClick = {
                        // TODO: navigate back later
                    },
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
}

@Preview
@Composable
private fun PreviewScreenPreview() {
    CofinanceTheme {
        Surface {
            PreviewScreen(
                imageUri = "https://google.com".toUri(),
                onNavigateToAdd = {},
                onNavigateBack = {}
            )
        }
    }
}