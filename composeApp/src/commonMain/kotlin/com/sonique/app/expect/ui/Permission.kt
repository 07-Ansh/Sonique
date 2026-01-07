package com.sonique.app.expect.ui

import androidx.compose.runtime.Composable

@Composable
expect fun rememberNotificationPermissionLauncher(onResult: (Boolean) -> Unit): () -> Unit
