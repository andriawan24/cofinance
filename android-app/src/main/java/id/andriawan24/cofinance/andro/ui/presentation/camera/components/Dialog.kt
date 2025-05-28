package id.andriawan24.cofinance.andro.ui.presentation.camera.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import id.andriawan24.cofinance.andro.R

@Composable
fun RationalPermissionDialog(onDialogDismissed: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDialogDismissed,
        title = {
            Text(
                text = stringResource(R.string.title_camera_permission_denied),
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Text(
                text = stringResource(R.string.description_camera_permission_denied),
                style = MaterialTheme.typography.bodySmall
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDialogDismissed()
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            ) {
                Text(text = stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDialogDismissed) {
                Text(text = stringResource(R.string.action_cancel))
            }
        }
    )
}