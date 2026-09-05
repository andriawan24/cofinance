package id.andriawan.cofinance.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import cofinance.composeapp.generated.resources.Res
import cofinance.composeapp.generated.resources.action_delete
import cofinance.composeapp.generated.resources.description_camera_permission_denied
import cofinance.composeapp.generated.resources.description_delete_account
import cofinance.composeapp.generated.resources.description_delete_transaction
import cofinance.composeapp.generated.resources.label_cancel
import cofinance.composeapp.generated.resources.label_ok
import cofinance.composeapp.generated.resources.title_camera_permission_denied
import cofinance.composeapp.generated.resources.title_delete_account
import cofinance.composeapp.generated.resources.title_delete_transaction
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

/**
 * The confirmation that stands between a swipe (or the edit screen's delete button) and a removal
 * that cannot be undone. Both entry points share it so the wording and the destructive styling of
 * the confirm action stay the same wherever the deletion starts.
 */
@Composable
fun DeleteTransactionDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    DestructiveConfirmationDialog(
        title = stringResource(Res.string.title_delete_transaction),
        description = stringResource(Res.string.description_delete_transaction),
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

/**
 * The confirmation for deleting an account, which takes every transaction recorded against it along
 * with it. The description says so, because the account list gives no hint of how much history hangs
 * off a given account.
 */
@Composable
fun DeleteAccountDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    DestructiveConfirmationDialog(
        title = stringResource(Res.string.title_delete_account),
        description = stringResource(Res.string.description_delete_account),
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
private fun DestructiveConfirmationDialog(
    title: String,
    description: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Text(text = description, style = MaterialTheme.typography.bodySmall)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(Res.string.action_delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.label_cancel))
            }
        }
    )
}
