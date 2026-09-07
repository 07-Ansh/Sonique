package com.sonique.app.expect

expect suspend fun saveImageToDevice(
    bytes: ByteArray,
    fileName: String,
): Boolean

expect suspend fun shareImage(
    bytes: ByteArray,
    fileName: String,
    chooserTitle: String,
): Boolean
