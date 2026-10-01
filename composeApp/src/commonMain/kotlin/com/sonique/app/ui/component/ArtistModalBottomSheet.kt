package com.sonique.app.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.sonique.app.ui.navigation.destination.list.ArtistDestination
import com.sonique.app.ui.theme.md_theme_dark_background
import com.sonique.app.ui.theme.typo
import com.sonique.domain.data.model.searchResult.songs.Artist
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.baseline_people_alt_24

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistModalBottomSheet(
    isBottomSheetVisible: Boolean,
    artists: List<Artist>,
    navController: NavController,
    onNavigateToOtherScreen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val modelBottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    val hideModalBottomSheet: () -> Unit =
        {
            coroutineScope.launch {
                modelBottomSheetState.hide()
                onDismiss()
            }
        }
    if (isBottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = modelBottomSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent,
            dragHandle = null,
            scrimColor = md_theme_dark_background.copy(alpha = .5f),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        ) {
            Card(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                colors = CardDefaults.cardColors().copy(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Card(
                        modifier =
                            Modifier
                                .width(60.dp)
                                .height(4.dp),
                        colors =
                            CardDefaults.cardColors().copy(
                                containerColor = Color(0xFF474545),
                            ),
                        shape = RoundedCornerShape(50),
                    ) {}
                    Spacer(modifier = Modifier.height(5.dp))
                    LazyColumn {
                        items(artists) { artist ->
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val id = artist.id
                                            if (!id.isNullOrBlank()) {
                                                onNavigateToOtherScreen()
                                                navController.navigate(
                                                    ArtistDestination(
                                                        id,
                                                    ),
                                                )
                                            }
                                        },
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier =
                                        Modifier
                                            .padding(20.dp)
                                            .align(Alignment.CenterStart),
                                ) {
                                    Image(
                                        painter =
                                            painterResource(
                                                Res.drawable.baseline_people_alt_24,
                                            ),
                                        contentDescription = "",
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = artist.name,
                                        style = typo().labelSmall,
                                    )
                                }
                            }
                        }
                        item {
                            EndOfModalBottomSheet()
                        }
                    }
                }
            }
        }
    }
}
