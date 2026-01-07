package com.sonique.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import com.sonique.common.LibraryChipType
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sonique.app.extension.NonLazyGrid
import com.sonique.app.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.sonique.app.ui.screen.library.LibraryDynamicPlaylistType
import com.sonique.app.ui.theme.musica_accent
import com.sonique.app.ui.theme.typo
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.downloaded
import sonique.composeapp.generated.resources.liked
import sonique.composeapp.generated.resources.playlists
import sonique.composeapp.generated.resources.your_youtube_playlists
import sonique.composeapp.generated.resources.followed
import sonique.composeapp.generated.resources.mix_for_you

@Composable
fun LibraryTilingBox(
    navController: NavController,
    onNavigate: (LibraryChipType) -> Unit
) {
    val listItem =
        listOf(
            LibraryTilingState.Downloaded,
            LibraryTilingState.Playlists,
            LibraryTilingState.MixForYou,
            LibraryTilingState.YouTubePlaylists,
            LibraryTilingState.Followed,
        )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        listItem.forEach { state ->
            LibraryTilingItem(
                state,
                onClick = {
                    when (state) {
                        LibraryTilingState.Favorite -> {
                            onNavigate(LibraryChipType.FAVORITE_PLAYLIST)
                        }

                        LibraryTilingState.Followed -> {
                           // Assuming we treat Followed as Artists or similar
                            navController.navigate(
                                LibraryDynamicPlaylistDestination(
                                    type = LibraryDynamicPlaylistType.Followed.toStringParams(),
                                ),
                            )
                        }

                        LibraryTilingState.Playlists -> {
                            onNavigate(LibraryChipType.LOCAL_PLAYLIST)
                        }

                        LibraryTilingState.YouTubePlaylists -> {
                            onNavigate(LibraryChipType.YOUTUBE_MUSIC_PLAYLIST)
                        }

                        LibraryTilingState.MixForYou -> {
                            onNavigate(LibraryChipType.YOUTUBE_MIX_FOR_YOU)
                        }

                        LibraryTilingState.Downloaded -> {
                            navController.navigate(
                                LibraryDynamicPlaylistDestination(
                                    type = LibraryDynamicPlaylistType.Downloaded.toStringParams(),
                                ),
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
fun LibraryTilingItem(
    state: LibraryTilingState,
    onClick: () -> Unit = {},
) {
    val title = stringResource(state.title)
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(50))
                .background(state.containerColor)
                .clickable {
                    onClick.invoke()
                }
                .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            title,
            style = typo().bodyMedium,
            color = Color.White,
        )
    }
}

data class LibraryTilingState(
    val title: StringResource,
    val containerColor: Color,
    val icon: ImageVector,
    val iconColor: Color,
) {
    companion object {
        val Favorite =
            LibraryTilingState(
                title = Res.string.liked,
                containerColor = Color(0xFF2C2C2C),
                icon = Icons.Default.Favorite,
                iconColor = musica_accent,
            )
        val Followed =
            LibraryTilingState(
                title = Res.string.followed,
                containerColor = Color(0xFF2C2C2C),
                icon = Icons.Default.Insights,
                iconColor = musica_accent,
            )
        val Playlists =
            LibraryTilingState(
                title = Res.string.playlists,
                containerColor = Color(0xFF2C2C2C),
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                iconColor = musica_accent,
            )
        val Downloaded =
            LibraryTilingState(
                title = Res.string.downloaded,
                containerColor = Color(0xFF2C2C2C),
                icon = Icons.Default.Downloading,
                iconColor = musica_accent,
            )
        val YouTubePlaylists =
            LibraryTilingState(
                title = Res.string.your_youtube_playlists,
                containerColor = Color(0xFF2C2C2C),
                icon = Icons.Default.Downloading, // Icon unused in UI refactor
                iconColor = musica_accent,
            )
        val MixForYou =
            LibraryTilingState(
                title = Res.string.mix_for_you,
                containerColor = Color(0xFF2C2C2C),
                icon = Icons.Default.Downloading, // Icon unused
                iconColor = musica_accent,
            )
    }
}

