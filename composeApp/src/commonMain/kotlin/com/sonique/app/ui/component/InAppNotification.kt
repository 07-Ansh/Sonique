package com.sonique.app.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sonique.app.ui.theme.typo

@Composable
fun InAppNotification(
    visible: Boolean,
    message: String,
    actionLabel: String? = null,
    onActionClick: () -> Unit = {},
    backgroundColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .padding(8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = message,
                    style = typo().bodyMedium,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (actionLabel != null) {
                    Button(
                        onClick = onActionClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = actionLabel,
                            style = typo().labelLarge
                        )
                    }
                }
            }
        }
    }
}


