package com.sonique.lyrics.romanization

/**
 * Desktop still ships the dictionary on the classpath — a desktop install is not an APK anyone is
 * counting megabytes on — so the pack is ready from the first frame and there is never anything
 * to download. [PlatformRomanizer]'s jvm actual keeps building the plain classpath `Tokenizer`.
 */
actual object RomanizationDictionaryPack {
    actual fun configure(directoryPath: String) {
        // Nothing to point anywhere: the dictionary is inside the jar.
    }

    actual fun isReady(): Boolean = true

    actual suspend fun download(): Result<Unit> = Result.success(Unit)
}
