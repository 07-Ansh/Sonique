package com.sonique.app.expect.ui

import androidx.compose.runtime.Composable

@Composable
actual fun rememberNotificationPermissionLauncher(onResult: (Boolean) -> Unit): () -> Unit {
    return {
         
        onResult(true)
    }
}
