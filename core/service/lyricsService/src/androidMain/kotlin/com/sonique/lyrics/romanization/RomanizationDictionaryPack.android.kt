package com.sonique.lyrics.romanization

import java.io.File

/**
 * Android is the one platform where the dictionary genuinely lives outside the app, so this is
 * the one actual with state: the configured directory, held here and read by [PlatformRomanizer]
 * when it decides whether it can build the analyzer. The heavy lifting — download, checksum,
 * unpacking, the kuromoji resolver — is all [KuromojiDictionary]'s.
 */
actual object RomanizationDictionaryPack {
    /** Written once at startup by the repository's constructor, read on every romanize of a Japanese line. */
    @Volatile
    internal var dictionaryDirectory: File? = null
        private set

    actual fun configure(directoryPath: String) {
        dictionaryDirectory = File(directoryPath)
    }

    actual fun isReady(): Boolean {
        val directory = dictionaryDirectory ?: return false
        return KuromojiDictionary.isReady(directory)
    }

    actual suspend fun download(): Result<Unit> {
        val directory =
            dictionaryDirectory
                ?: return Result.failure(IllegalStateException("RomanizationDictionaryPack.configure was never called"))
        return KuromojiDictionary.download(directory)
    }
}
