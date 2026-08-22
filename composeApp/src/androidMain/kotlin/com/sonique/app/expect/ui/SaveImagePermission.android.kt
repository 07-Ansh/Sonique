package com.sonique.app.expect.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
actual fun rememberSaveImagePermission(onResult: (granted: Boolean) -> Unit): SaveImagePermissionRequester {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            currentOnResult(granted)
        }

    return remember(context, launcher) {
        object : SaveImagePermissionRequester {
            override fun requestIfNeeded() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    currentOnResult(true)
                    return
                }
                val alreadyGranted =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    ) == PackageManager.PERMISSION_GRANTED

                if (alreadyGranted) {
                    currentOnResult(true)
                } else {
                    launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
    }
}
