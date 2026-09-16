package com.sonique.app.ui.component

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GoogleCircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    LoadingIndicator(
        modifier = modifier,
        color = color
    )
}
