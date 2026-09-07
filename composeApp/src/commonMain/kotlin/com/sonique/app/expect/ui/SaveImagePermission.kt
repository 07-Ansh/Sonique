package com.sonique.app.expect.ui

import androidx.compose.runtime.Composable

interface SaveImagePermissionRequester {
    fun requestIfNeeded()
}

@Composable
expect fun rememberSaveImagePermission(onResult: (granted: Boolean) -> Unit): SaveImagePermissionRequester
