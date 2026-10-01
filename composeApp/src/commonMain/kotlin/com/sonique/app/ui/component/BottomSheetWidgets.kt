package com.sonique.app.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.sonique.app.extension.greyScale
import com.sonique.app.ui.theme.typo
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.baseline_favorite_24
import sonique.composeapp.generated.resources.baseline_favorite_border_24
import sonique.composeapp.generated.resources.like
import sonique.composeapp.generated.resources.liked
import sonique.composeapp.generated.resources.your_sp_dc_param_of_spotify_cookie
import sonique.composeapp.generated.resources.your_youtube_cookie

@Composable
fun ActionButton(
    icon: Painter,
    text: StringResource?,
    textString: String? = null,
    textColor: Color? = null,
    iconColor: Color = Color.White,
    enable: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .wrapContentHeight(Alignment.CenterVertically)
                .then(
                    if (enable) Modifier.clickable { onClick.invoke() } else Modifier.greyScale(),
                ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp),
        ) {
            Image(
                painter = icon,
                contentDescription = if (text != null) stringResource(text) else textString ?: "",
                modifier =
                    Modifier
                        .wrapContentSize(
                            Alignment.Center,
                        ).padding(12.dp),
                colorFilter =
                    if (enable) {
                        ColorFilter.tint(iconColor)
                    } else {
                        ColorFilter.tint(Color.Gray)
                    },
            )

            Text(
                text = if (text != null) stringResource(text) else textString ?: "",
                style = typo().labelSmall,
                color = if (enable) textColor ?: Color.Unspecified else Color.Gray,
                modifier =
                    Modifier
                        .padding(start = 10.dp)
                        .wrapContentHeight(Alignment.CenterVertically),
            )
        }
    }
}

@Composable
fun CheckBoxActionButton(
    defaultChecked: Boolean,
    isHeartIcon: Boolean,
    onChangeListener: (checked: Boolean) -> Unit,
) {
    var stateChecked by remember { mutableStateOf(defaultChecked) }
    Box(
        modifier =
            Modifier
                .wrapContentSize(align = Alignment.Center)
                .clickable {
                    stateChecked = !stateChecked
                    onChangeListener(stateChecked)
                },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
        ) {
            Box(Modifier.padding(10.dp)) {
                if (isHeartIcon) {
                    HeartCheckBox(checked = stateChecked, size = 30)
                } else {
                    Crossfade(stateChecked) {
                        if (it) {
                            Icon(Icons.Rounded.CheckCircle, "")
                        } else {
                            Icon(Icons.Rounded.AddCircleOutline, "")
                        }
                    }
                }
            }
            Text(
                text =
                    if (stateChecked) {
                        stringResource(
                            Res.string.liked,
                        )
                    } else {
                        stringResource(Res.string.like)
                    },
                style = typo().labelSmall,
                modifier =
                    Modifier
                        .padding(start = 10.dp)
                        .wrapContentHeight(Alignment.CenterVertically),
            )
        }
    }
}

@Composable
fun HeartCheckBox(
    size: Int = 24,
    checked: Boolean,
    onStateChange: (() -> Unit)? = null,
) {
    Box(
        modifier =
            Modifier
                .size(size.dp)
                .clip(
                    CircleShape,
                ).clickable {
                    onStateChange?.invoke()
                },
    ) {
        Crossfade(targetState = checked, modifier = Modifier.fillMaxSize()) {
            if (it) {
                Image(
                    painter = painterResource(Res.drawable.baseline_favorite_24),
                    contentDescription = "Favorite checked",
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                )
            } else {
                Image(
                    painter = painterResource(Res.drawable.baseline_favorite_border_24),
                    contentDescription = "Favorite unchecked",
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                    colorFilter = ColorFilter.tint(Color.White),
                )
            }
        }
    }
}

@Composable
fun EndOfModalBottomSheet() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(
                    WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding()
                        .value
                        .toInt()
                        .dp + 8.dp,
                ),
    ) {}
}

sealed class DevLogInType {
    data object Spotify : DevLogInType()

    data object YouTube : DevLogInType()

    @Composable
    fun getTitleString(): String =
        when (this) {
            is Spotify -> stringResource(Res.string.your_sp_dc_param_of_spotify_cookie)
            is YouTube -> stringResource(Res.string.your_youtube_cookie)
        }

    suspend fun getTitle(): String =
        when (this) {
            is Spotify -> getString(Res.string.your_sp_dc_param_of_spotify_cookie)
            is YouTube -> getString(Res.string.your_youtube_cookie)
        }
}
