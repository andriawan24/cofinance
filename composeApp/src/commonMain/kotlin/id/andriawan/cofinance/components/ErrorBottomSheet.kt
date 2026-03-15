package id.andriawan.cofinance.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.action_dismiss
import cofinance.composeapp.generated.resources.label_error
import id.andriawan.cofinance.utils.Dimensions
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorBottomSheet(
    message: String?,
    onDismiss: () -> Unit
) {
    if (message == null) return

    val sheetState = rememberModalBottomSheetState()

    BaseBottomSheet(
        state = sheetState,
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimensions.SIZE_24)
                .padding(bottom = Dimensions.SIZE_40),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(Dimensions.SIZE_48)
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "!",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                )
            }

            Spacer(modifier = Modifier.height(Dimensions.SIZE_16))

            Text(
                text = stringResource(Res.string.label_error),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(Dimensions.SIZE_8))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(Dimensions.SIZE_24))

            PrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismiss
            ) {
                Text(
                    text = stringResource(Res.string.action_dismiss),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}
