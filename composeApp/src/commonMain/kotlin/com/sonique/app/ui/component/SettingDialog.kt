package com.sonique.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.sonique.app.viewModel.SettingAlertState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingDialog(
    alert: SettingAlertState,
    onDismiss: () -> Unit
) {
    var textFieldValue by remember { mutableStateOf(alert.textField?.value ?: "") }
    var currentSelectOne by remember { mutableStateOf(alert.selectOne?.listSelect?.map { it.first to it.second }) }
    var currentMultipleSelect by remember { mutableStateOf(alert.multipleSelect?.listSelect?.map { it.first to it.second }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = alert.title, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column {
                if (!alert.message.isNullOrEmpty()) {
                    Text(text = alert.message, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (alert.textField != null) {
                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = { textFieldValue = it },
                        label = { Text(alert.textField.label) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                if (alert.selectOne != null) {
                    LazyColumn {
                        items(currentSelectOne ?: emptyList()) { (selected, text) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .selectable(
                                        selected = selected,
                                        onClick = {
                                            currentSelectOne = currentSelectOne?.map { (s, t) ->
                                                (t == text) to t
                                            }
                                        },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = null 
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

                if (alert.multipleSelect != null) {
                    LazyColumn {
                        items(currentMultipleSelect ?: emptyList()) { (selected, text) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clickable {
                                        currentMultipleSelect = currentMultipleSelect?.map { (s, t) ->
                                            if (t == text) !s to t else s to t
                                        }
                                    }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newState = alert.copy(
                        textField = alert.textField?.copy(value = textFieldValue),
                        selectOne = alert.selectOne?.copy(listSelect = currentSelectOne ?: emptyList()),
                        multipleSelect = alert.multipleSelect?.copy(listSelect = currentMultipleSelect ?: emptyList())
                    )
                    alert.confirm.second(newState)
                    onDismiss()
                }
            ) {
                Text(alert.confirm.first)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(alert.dismiss)
            }
        }
    )
}
