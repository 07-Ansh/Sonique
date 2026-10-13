package com.sonique.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sonique.app.expect.ui.PlatformBackdrop
import com.sonique.app.expect.ui.rememberBackdrop
import com.sonique.app.ui.theme.typo
import com.sonique.app.viewModel.SharedViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.play_all

/**
 * A themed pill-shaped "Play all" button that adapts between
 * Liquid Glass mode (frosted interactive glass with border) and Normal mode (Material 3 surfaceContainerHighest).
 */
@Composable
fun PlayAllButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enableLiquidGlass: Boolean? = null,
    backdrop: PlatformBackdrop? = null,
) {
    val sharedViewModel: SharedViewModel = koinInject()
    val isLiquidGlassPref by sharedViewModel.enableLiquidGlass.collectAsStateWithLifecycle()
    val isGlass = enableLiquidGlass ?: isLiquidGlassPref

    if (isGlass) {
        val actualBackdrop = backdrop ?: rememberBackdrop()
        LiquidGlassContainer(
            backdrop = actualBackdrop,
            modifier = modifier
                .height(34.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.12f)), CircleShape)
                .clickable { onClick() },
            shape = CircleShape,
            interactive = true,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = stringResource(Res.string.play_all),
                    style = typo().labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    } else {
        FilledTonalButton(
            onClick = onClick,
            modifier = modifier.height(34.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = stringResource(Res.string.play_all),
                style = typo().labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
