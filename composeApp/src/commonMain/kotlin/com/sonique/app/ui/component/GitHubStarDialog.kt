package com.sonique.app.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon

@Composable
fun GitHubStarDialog(
    onDismiss: () -> Unit,
    onStar: () -> Unit,
    onNeverShowAgain: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val githubUrl = "https://github.com/07-Ansh/Sonique"

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Rounded.Star, contentDescription = null)
        },
        title = {
            Text(text = "Enjoying Sonique?")
        },
        text = {
            Text(text = "If you like using Sonique, please consider giving us a star on GitHub! It helps us a lot.")
        },
        confirmButton = {
            Button(
                onClick = {
                    uriHandler.openUri(githubUrl)
                    onStar()
                }
            ) {
                Text("Star on GitHub")
            }
        },
        dismissButton = {
            TextButton(onClick = onNeverShowAgain) {
                Text("Don't ask again")
            }
        },
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}
