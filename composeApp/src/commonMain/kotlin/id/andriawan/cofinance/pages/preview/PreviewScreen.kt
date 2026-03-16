package id.andriawan.cofinance.pages.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.StrokeCap
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.action_retake_photo
import cofinance.composeapp.generated.resources.action_use_photo
import cofinance.composeapp.generated.resources.action_download
import cofinance.composeapp.generated.resources.action_later
import cofinance.composeapp.generated.resources.ic_arrow_left
import cofinance.composeapp.generated.resources.title_download_required
import cofinance.composeapp.generated.resources.description_download_required
import cofinance.composeapp.generated.resources.label_downloading_model
import cofinance.composeapp.generated.resources.label_analyzing_receipt
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.diamondedge.logging.logging
import id.andriawan.cofinance.components.ErrorBottomSheet
import id.andriawan.cofinance.components.PrimaryButton
import id.andriawan.cofinance.theme.CofinanceTheme
import id.andriawan.cofinance.utils.UiText
import id.andriawan.cofinance.utils.Dimensions
import id.andriawan.cofinance.utils.deleteFile
import id.andriawan.cofinance.utils.extensions.CollectAsEffect
import id.andriawan.cofinance.utils.readFromFile
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    imageUrl: String,
    onNavigateToAdd: (String) -> Unit,
    onNavigateBack: () -> Unit,
    previewViewModel: PreviewViewModel = koinViewModel()
) {
    val context = LocalPlatformContext.current
    val uiState by previewViewModel.previewUiState.collectAsStateWithLifecycle()
    val imageFile = remember { readFromFile(context, imageUrl) }
    var errorUiText by remember { mutableStateOf<UiText?>(null) }

    DisposableEffect(imageUrl) {
        onDispose { deleteFile(imageUrl) }
    }

    previewViewModel.previewUiEvent.CollectAsEffect {
        when (it) {
            is PreviewUiEvent.NavigateToBalance -> onNavigateToAdd(it.transactionId)
            is PreviewUiEvent.ShowMessage -> errorUiText = it.message
        }
    }

    Scaffold { contentPadding ->
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
                    painter = painterResource(Res.drawable.ic_arrow_left),
                    contentDescription = null
                )
            }

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Spacer(modifier = Modifier.height(Dimensions.SIZE_24))

                AsyncImage(
                    modifier = Modifier.fillMaxWidth(),
                    model = ImageRequest.Builder(context)
                        .data(imageFile)
                        .crossfade(200)
                        .build(),
                    contentScale = ContentScale.FillWidth,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.height(Dimensions.SIZE_40))

                // Show download progress or loading indicator
                if (uiState.showLoading) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimensions.SIZE_16),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8)
                    ) {
                        if (uiState.downloadProgress != null) {
                            Text(
                                text = stringResource(
                                    Res.string.label_downloading_model,
                                    (uiState.downloadProgress!! * 100).toInt()
                                ),
                                style = MaterialTheme.typography.labelMedium
                            )
                            LinearProgressIndicator(
                                progress = { uiState.downloadProgress!! },
                                modifier = Modifier.fillMaxWidth(),
                                strokeCap = StrokeCap.Round
                            )
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_8),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(Dimensions.SIZE_16),
                                    strokeWidth = Dimensions.SIZE_2
                                )
                                Text(
                                    text = stringResource(Res.string.label_analyzing_receipt),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Dimensions.SIZE_16))
                    }
                }

                PrimaryButton(
                    modifier = Modifier
                        .dropShadow(
                            shape = RectangleShape,
                            shadow = Shadow(
                                radius = Dimensions.SIZE_10,
                                spread = Dimensions.SIZE_2,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                offset = DpOffset(x = Dimensions.zero, y = Dimensions.SIZE_4)
                            )
                        )
                        .fillMaxWidth()
                        .padding(horizontal = Dimensions.SIZE_16),
                    onClick = {
                        previewViewModel.scanReceipt(file = imageFile)
                    },
                    enabled = !uiState.showLoading
                ) {
                    Text(
                        text = stringResource(Res.string.action_use_photo),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(Dimensions.SIZE_16))

                TextButton(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    contentPadding = PaddingValues(
                        horizontal = Dimensions.SIZE_24,
                        vertical = Dimensions.SIZE_16
                    ),
                    onClick = onNavigateBack,
                    enabled = !uiState.showLoading
                ) {
                    Text(
                        text = stringResource(Res.string.action_retake_photo),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }

    // Download Required Bottom Sheet
    if (uiState.showDownloadPrompt) {
        ModalBottomSheet(
            onDismissRequest = { previewViewModel.dismissDownloadPrompt() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.SIZE_24)
                    .padding(bottom = Dimensions.SIZE_24)
            ) {
                Text(
                    text = stringResource(Res.string.title_download_required),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(Dimensions.SIZE_8))

                Text(
                    text = stringResource(Res.string.description_download_required),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(Dimensions.SIZE_24))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimensions.SIZE_12)
                ) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        onClick = { previewViewModel.dismissDownloadPrompt() }
                    ) {
                        Text(stringResource(Res.string.action_later))
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { previewViewModel.startDownloadAndScan() }
                    ) {
                        Text(stringResource(Res.string.action_download))
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

@Preview
@Composable
private fun PreviewScreenPreview() {
    CofinanceTheme {
        Surface {
            PreviewScreen(
                imageUrl = "https://google.com",
                onNavigateToAdd = {},
                onNavigateBack = {}
            )
        }
    }
}
