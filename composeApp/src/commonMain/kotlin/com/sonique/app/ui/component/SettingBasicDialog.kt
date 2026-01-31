package com.sonique.app.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.sonique.app.viewModel.SettingBasicAlertState

@Composable
fun SettingBasicDialog(
    alert: SettingBasicAlertState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = alert.title)
        },
        text = {
            if (!alert.message.isNullOrEmpty()) {
                Text(text = alert.message)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    alert.confirm.second()
                    onDismiss()
                }
            ) {
                Text(alert.confirm.first)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(alert.dismiss)
            }
        }
    )
}
