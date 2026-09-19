package com.sonique.app.expect

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.sonique.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform.getKoin
import java.io.File

private const val TAG = "ShareImage"
private const val MIME_PNG = "image/png"
private const val ALBUM = "Sonique"

actual suspend fun saveImageToDevice(
    bytes: ByteArray,
    fileName: String,
): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val context: Context = getKoin().get()
            val resolver = context.contentResolver
            val isScopedStorage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

            val values =
                ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, MIME_PNG)
                    if (isScopedStorage) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    } else {
                        @Suppress("DEPRECATION")
                        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ALBUM).apply { mkdirs() }
                        val file = File(dir, fileName)
                        @Suppress("DEPRECATION")
                        put(MediaStore.Images.Media.DATA, file.absolutePath)
                    }
                }

            val uri =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@runCatching false

            resolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: return@runCatching false

            if (isScopedStorage) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        }.getOrElse { error ->
            Logger.e(TAG, "Could not save $fileName to gallery: ${error.message}", error)
            false
        }
    }

actual suspend fun shareImage(
    bytes: ByteArray,
    fileName: String,
    chooserTitle: String,
): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val context: Context = getKoin().get()
            val dir = File(context.cacheDir, "shared_images").apply { mkdirs() }
            val file = File(dir, fileName)
            file.writeBytes(bytes)

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", file)
            val sendIntent =
                Intent(Intent.ACTION_SEND).apply {
                    type = MIME_PNG
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            val chooser = Intent.createChooser(sendIntent, chooserTitle).apply { addFlags(FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(chooser)
            true
        }.getOrElse { error ->
            Logger.e(TAG, "Could not share $fileName: ${error.message}", error)
            false
        }
    }
