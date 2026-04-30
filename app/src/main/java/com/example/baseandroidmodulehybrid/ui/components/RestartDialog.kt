package com.example.baseandroidmodulehybrid.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.baseandroidmodulehybrid.R

@Composable
fun RestartDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.restart_dialog_title)) },
        text = { Text(stringResource(R.string.restart_dialog_text)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.restart_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.restart_later))
            }
        }
    )
}
