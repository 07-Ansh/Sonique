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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import org.jetbrains.compose.resources.stringResource
import sonique.composeapp.generated.resources.*

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
            Text(text = stringResource(Res.string.enjoying_sonique))
        },
        text = {
            Text(text = stringResource(Res.string.enjoying_sonique_description))
        },
        confirmButton = {
            Button(
                onClick = {
                    uriHandler.openUri(githubUrl)
                    onStar()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = stringResource(Res.string.star_on_github),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onNeverShowAgain) {
                Text(stringResource(Res.string.dont_ask_again))
            }
        },
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
        textContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
    )
}
