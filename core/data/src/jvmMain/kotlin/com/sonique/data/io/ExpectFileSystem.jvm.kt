package com.sonique.data.io

import okio.FileSystem
import java.io.File


actual fun fileSystem(): FileSystem {
    return FileSystem.SYSTEM
}

actual fun fileDir(): String = File(getHomeFolderPath(listOf(".com.sonique.com.sonique.app"))).absolutePath

