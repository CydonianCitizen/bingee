package com.cydoniancitizen.bingee.app

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cydoniancitizen.bingee.R
import com.cydoniancitizen.bingee.domain.repository.AppUpdateResult

@Composable
internal fun AppUpdateDialog(
    update: AppUpdateResult.UpdateAvailable?,
    onDismiss: () -> Unit,
    onViewRelease: (String) -> Unit
) {
    if (update == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_available)) },
        text = { Text(stringResource(R.string.update_available_message, update.latestVersion)) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onViewRelease(update.releaseUrl)
            }) {
                Text(stringResource(R.string.update_view_release))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.update_later))
            }
        }
    )
}
