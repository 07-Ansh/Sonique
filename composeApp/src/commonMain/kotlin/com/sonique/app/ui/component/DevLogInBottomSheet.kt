package com.sonique.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sonique.app.expect.copyToClipboard
import com.sonique.app.ui.theme.typo
import org.jetbrains.compose.resources.stringResource
import sonique.composeapp.generated.resources.Res
import sonique.composeapp.generated.resources.can_not_be_empty
import sonique.composeapp.generated.resources.copied_to_clipboard
import sonique.composeapp.generated.resources.list_all_cookies_of_this_page
import sonique.composeapp.generated.resources.processing
import sonique.composeapp.generated.resources.set

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevLogInBottomSheet(
    onDismiss: () -> Unit,
    type: DevLogInType,
    onDone: (String, String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val modelBottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )

    var value by rememberSaveable { mutableStateOf("") }
    var secondValue by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modelBottomSheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
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
                            containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        ),
                    shape = RoundedCornerShape(50),
                ) {}
                Spacer(modifier = Modifier.height(10.dp))
                val processingString = stringResource(Res.string.processing)
                val cannotBeEmptyString = stringResource(Res.string.can_not_be_empty)
                Text(text = type.getTitleString(), style = typo().labelSmall)
                Spacer(modifier = Modifier.height(5.dp))
                OutlinedTextField(
                    value = value,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    onValueChange = { value = it },
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(5.dp))
                if (type == DevLogInType.YouTube) {
                    Text(text = "Netscape cookie", style = typo().labelSmall)
                    Spacer(modifier = Modifier.height(5.dp))
                    OutlinedTextField(
                        value = secondValue,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                        onValueChange = { secondValue = it },
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                }
                TextButton(
                    onClick = {
                        if (value.isNotEmpty() && value.isNotBlank() &&
                            (type != DevLogInType.YouTube || (secondValue.isNotEmpty() && secondValue.isNotBlank()))
                        ) {
                            SoniqueToastManager.show(processingString)
                            onDismiss()
                            onDone(value, secondValue)
                        } else {
                            SoniqueToastManager.show(cannotBeEmptyString)
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                ) {
                    Text(text = stringResource(Res.string.set), style = typo().labelSmall)
                }
                Spacer(modifier = Modifier.height(5.dp))
                EndOfModalBottomSheet()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevCookieLogInBottomSheet(
    onDismiss: () -> Unit,
    type: DevLogInType,
    cookies: List<Pair<String, String?>>,
) {
    val coroutineScope = rememberCoroutineScope()
    val modelBottomSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modelBottomSheetState,
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = .5f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            colors = CardDefaults.cardColors().copy(containerColor = Color(0xFF242424)),
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
                            containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        ),
                    shape = RoundedCornerShape(50),
                ) {}
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = stringResource(Res.string.list_all_cookies_of_this_page), style = typo().labelSmall)
                cookies.forEach { cookie ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Text(
                            text = cookie.first,
                            style = typo().bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        SelectionContainer(
                            modifier = Modifier.weight(2f),
                        ) {
                            Text(
                                text = cookie.second ?: "",
                                style = typo().bodyMedium,
                            )
                        }
                        val copied = stringResource(Res.string.copied_to_clipboard)
                        IconButton(
                            onClick = {
                                copyToClipboard(cookie.first, cookie.second ?: "")
                                SoniqueToastManager.show(copied)
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                            )
                        }
                    }
                }
                EndOfModalBottomSheet()
            }
        }
    }
}
