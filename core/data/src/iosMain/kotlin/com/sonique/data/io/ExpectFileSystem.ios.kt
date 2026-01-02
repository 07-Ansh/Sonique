package com.sonique.data.io

import com.sonique.data.db.documentDirectory
import okio.FileSystem

actual fun fileSystem(): FileSystem = FileSystem.SYSTEM
actual fun fileDir(): String = documentDirectory() + "/SimpMusic"

