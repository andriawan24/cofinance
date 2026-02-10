package id.andriawan.cofinance.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.description_camera_permission_denied
import cofinance.composeapp.generated.resources.label_cancel
import cofinance.composeapp.generated.resources.label_ok
import cofinance.composeapp.generated.resources.title_camera_permission_denied
import coil3.compose.LocalPlatformContext
import id.andriawan.cofinance.goToSystemSettings
import org.jetbrains.compose.resources.stringResource

@Composable
fun RationalPermissionDialog(onDialogDismissed: () -> Unit) {
    val context = LocalPlatformContext.current

    AlertDialog(
        onDismissRequest = onDialogDismissed,
        title = {
            Text(
                text = stringResource(Res.string.title_camera_permission_denied),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Text(
                text = stringResource(Res.string.description_camera_permission_denied),
                style = MaterialTheme.typography.bodySmall
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDialogDismissed()
                    context.goToSystemSettings()
                }
            ) {
                Text(text = stringResource(Res.string.label_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDialogDismissed) {
                Text(text = stringResource(Res.string.label_cancel))
            }
        }
    )
}
