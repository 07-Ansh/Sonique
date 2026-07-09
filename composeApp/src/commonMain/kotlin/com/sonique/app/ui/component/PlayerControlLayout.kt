package com.sonique.app.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sonique.domain.mediaservice.handler.ControlState
import com.sonique.domain.mediaservice.handler.RepeatState
import com.sonique.app.ui.theme.seed
import com.sonique.app.ui.theme.transparent
import com.sonique.app.viewModel.UIEvent

@Composable
fun PlayerControlLayout(
    controllerState: ControlState,
    isSmallSize: Boolean = false,
    enableExpressive: Boolean = false,
    onUIEvent: (UIEvent) -> Unit,
) {
    val contentColor = if (enableExpressive) Color(0xFFFAF9F6) else Color.White
    val height = if (isSmallSize) 48.dp else 96.dp
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(height)
                .padding(horizontal = 20.dp),
    ) {
        if (enableExpressive) {
            val mainHeight = if (isSmallSize) 48.dp else 80.dp
            val skipWidth = if (isSmallSize) 32.dp else 48.dp
            val playSize = if (isSmallSize) 48.dp else 80.dp
            val controlSize = if (isSmallSize) 32.dp else 48.dp
            val borderStroke = BorderStroke(1.dp, contentColor.copy(alpha = 0.2f))

            // 1. Shuffle
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(controlSize)
                        .border(borderStroke, CircleShape)
                        .clip(CircleShape)
                        .rubberyClick { onUIEvent(UIEvent.Shuffle) },
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(targetState = controllerState.isShuffle, label = "Shuffle Button") { isShuffle ->
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            tint = if (isShuffle) seed else Color.Gray,
                            contentDescription = "",
                            modifier = Modifier.size(if (isSmallSize) 18.dp else 24.dp),
                        )
                    }
                }
            }

            // 2. Skip Previous
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(width = skipWidth, height = mainHeight)
                        .background(contentColor.copy(alpha = 0.12f), RoundedCornerShape(percent = 50))
                        .clip(RoundedCornerShape(percent = 50))
                        .rubberyClick {
                            if (controllerState.isPreviousAvailable) {
                                onUIEvent(UIEvent.Previous)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        tint = if (controllerState.isPreviousAvailable) contentColor else Color.Gray,
                        contentDescription = "",
                        modifier = Modifier.size(if (isSmallSize) 20.dp else 28.dp),
                    )
                }
            }

            // 3. Play/Pause
            Box(Modifier.weight(1.2f), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(playSize)
                        .background(Color(0xFFFAF9F6), RoundedCornerShape(if (isSmallSize) 14.dp else 24.dp))
                        .clip(RoundedCornerShape(if (isSmallSize) 14.dp else 24.dp))
                        .rubberyClick { onUIEvent(UIEvent.PlayPause) },
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(targetState = controllerState.isPlaying) { isPlaying ->
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            tint = Color.Black,
                            contentDescription = "",
                            modifier = Modifier.size(if (isSmallSize) 28.dp else 40.dp),
                        )
                    }
                }
            }

            // 4. Skip Next
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(width = skipWidth, height = mainHeight)
                        .background(contentColor.copy(alpha = 0.12f), RoundedCornerShape(percent = 50))
                        .clip(RoundedCornerShape(percent = 50))
                        .rubberyClick {
                            if (controllerState.isNextAvailable) {
                                onUIEvent(UIEvent.Next)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        tint = if (controllerState.isNextAvailable) contentColor else Color.Gray,
                        contentDescription = "",
                        modifier = Modifier.size(if (isSmallSize) 20.dp else 28.dp),
                    )
                }
            }

            // 5. Repeat
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(controlSize)
                        .border(borderStroke, CircleShape)
                        .clip(CircleShape)
                        .rubberyClick { onUIEvent(UIEvent.Repeat) },
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(targetState = controllerState.repeatState) { rs ->
                        val icon = when (rs) {
                            RepeatState.One -> Icons.Rounded.RepeatOne
                            else -> Icons.Rounded.Repeat
                        }
                        val tint = when (rs) {
                            RepeatState.None -> Color.Gray
                            else -> seed
                        }
                        Icon(
                            imageVector = icon,
                            tint = tint,
                            contentDescription = "",
                            modifier = Modifier.size(if (isSmallSize) 18.dp else 24.dp),
                        )
                    }
                }
            }
        } else {
            val smallIcon = if (isSmallSize) 20.dp to 28.dp else 32.dp to 42.dp
            val mediumIcon = if (isSmallSize) 28.dp to 38.dp else 42.dp to 52.dp
            val bigIcon = if (isSmallSize) 38.dp to 48.dp else 72.dp to 96.dp

            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    modifier =
                        Modifier
                            .background(transparent)
                            .size(smallIcon.second)
                            .aspectRatio(1f)
                            .clip(
                                CircleShape,
                            )
                            .clickable {
                                onUIEvent(UIEvent.Shuffle)
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Crossfade(targetState = controllerState.isShuffle, label = "Shuffle Button") { isShuffle ->
                        if (!isShuffle) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                tint = Color.Gray,
                                contentDescription = "",
                                modifier = Modifier.size(smallIcon.first),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                tint = seed,
                                contentDescription = "",
                                modifier = Modifier.size(smallIcon.first),
                            )
                        }
                    }
                }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    modifier =
                        Modifier
                            .background(transparent)
                            .size(mediumIcon.second)
                            .aspectRatio(1f)
                            .clip(
                                CircleShape,
                            )
                            .clickable {
                                if (controllerState.isPreviousAvailable) {
                                    onUIEvent(UIEvent.Previous)
                                }
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        tint = if (controllerState.isPreviousAvailable) contentColor else Color.Gray,
                        contentDescription = "",
                        modifier = Modifier.size(mediumIcon.first),
                    )
                }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    modifier =
                        Modifier
                            .size(bigIcon.second)
                            .aspectRatio(1f)
                            .clip(
                                CircleShape,
                            )
                            .background(Color(0xFFE0E0E0))  
                            .clickable {
                                onUIEvent(UIEvent.PlayPause)
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Crossfade(targetState = controllerState.isPlaying) { isPlaying ->
                        if (!isPlaying) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                tint = Color.Black,
                                contentDescription = "",
                                modifier = Modifier.size(bigIcon.first),
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Pause,
                                tint = Color.Black,
                                contentDescription = "",
                                modifier = Modifier.size(bigIcon.first),
                            )
                        }
                    }
                }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    modifier =
                        Modifier
                            .background(transparent)
                            .size(mediumIcon.second)
                            .aspectRatio(1f)
                            .clip(
                                CircleShape,
                            )
                            .clickable {
                                if (controllerState.isNextAvailable) {
                                    onUIEvent(UIEvent.Next)
                                }
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        tint = if (controllerState.isNextAvailable) contentColor else Color.Gray,
                        contentDescription = "",
                        modifier = Modifier.size(mediumIcon.first),
                    )
                }
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    modifier =
                        Modifier
                            .size(smallIcon.second)
                            .aspectRatio(1f)
                            .clip(
                                CircleShape,
                            )
                            .clickable {
                                onUIEvent(UIEvent.Repeat)
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Crossfade(targetState = controllerState.repeatState) { rs ->
                        when (rs) {
                            is RepeatState.None -> {
                                Icon(
                                    imageVector = Icons.Rounded.Repeat,
                                    tint = Color.Gray,
                                    contentDescription = "",
                                    modifier = Modifier.size(smallIcon.first),
                                )
                            }

                            RepeatState.All -> {
                                Icon(
                                    imageVector = Icons.Rounded.Repeat,
                                    tint = seed,
                                    contentDescription = "",
                                    modifier = Modifier.size(smallIcon.first),
                                )
                            }

                            RepeatState.One -> {
                                Icon(
                                    imageVector = Icons.Rounded.RepeatOne,
                                    tint = seed,
                                    contentDescription = "",
                                    modifier = Modifier.size(smallIcon.first),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}



private fun Modifier.rubberyClick(
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1.0f,
        animationSpec = if (isPressed) {
            spring(
                dampingRatio = 1.0f, // No bounce on press down
                stiffness = 1000f    // Fast, responsive compression
            )
        } else {
            spring(
                dampingRatio = 0.35f, // Highly elastic release bounce
                stiffness = 350f     // Satisfying snap back with zoom overshoot
            )
        },
        label = "RubberyClickScale"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}
