package com.example.baseandroidmodulehybrid.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.baseandroidmodulehybrid.R
import com.example.baseandroidmodulehybrid.updater.UpdateState

@Composable
fun ReadyToInstallDialog(
    state: UpdateState.ReadyToInstall,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_dialog_title)) },
        text = { Text(stringResource(R.string.update_dialog_text, state.versionInfo.version)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.update_install))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.update_cancel))
            }
        }
    )
}
