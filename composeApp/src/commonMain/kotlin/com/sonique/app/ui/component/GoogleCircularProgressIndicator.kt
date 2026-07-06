package com.sonique.app.ui.component

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GoogleCircularProgressIndicator(
    modifier: Modifier = Modifier
) {
    LoadingIndicator(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary
    )
}
