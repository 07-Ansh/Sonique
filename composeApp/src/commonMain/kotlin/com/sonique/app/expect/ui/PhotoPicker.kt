@file:Suppress("ktlint:standard:filename")

package com.sonique.app.expect.ui

import androidx.compose.runtime.Composable

interface PhotoPickerLauncher {
    fun launch()
}

@Composable
expect fun photoPickerResult(onResultUri: (String?) -> Unit): PhotoPickerLauncher

