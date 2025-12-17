package com.valhalla.brokk.presentation.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.valhalla.brokk.R

@Composable
fun ExitDialog(
    onDismiss: (Boolean) -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            onDismiss(false)
        },
        title = {
            Text(text = stringResource(R.string.confirm_exit_title))
        },
        text = {
            Text(text = stringResource(R.string.confirm_exit_message))
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss(true)
                }
            ) {
                Text(stringResource(R.string.exit)) // e.g., "Exit"
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismiss(false)
                }
            ) {
                Text(stringResource(R.string.cancel)) // e.g., "Cancel"
            }
        }
    )
}