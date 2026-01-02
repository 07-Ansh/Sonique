package com.sonique.data.io

import android.content.Context
import okio.FileSystem
import org.koin.mp.KoinPlatform.getKoin

actual fun fileSystem(): FileSystem = FileSystem.SYSTEM
actual fun fileDir(): String {
    val context = getKoin().get<Context>()
    return context.filesDir.absolutePath
}

