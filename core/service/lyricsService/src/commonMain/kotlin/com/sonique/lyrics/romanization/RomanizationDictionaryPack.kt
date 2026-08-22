package com.sonique.lyrics.romanization

/**
 * The Japanese analyzer's dictionary pack, on platforms where it is not part of the app.
 *
 * kuromoji's ipadic dictionary is ~13 MB of `.bin` resources. On Android those are excluded from
 * the APK and fetched once, on demand; on Desktop they still ship on the classpath, and iOS has no
 * analyzer at all — both of those answer [isReady] with true so nothing upstream ever asks them to
 * download. Same expect/actual shape as [PlatformRomanizer], which is this module's existing way
 * of splitting the two platform-bound romanizers from the ten pure-Kotlin ones.
 */
expect object RomanizationDictionaryPack {
    /**
     * Tells the pack where its files live (or should be installed). Android reads and writes
     * `<directoryPath>` — everyone else ignores it. Must be called before [isReady] or [download]
     * mean anything on Android; the repository in `data` does so from its own constructor, which
     * is the only path to the romanizer, so no romanize call can precede it.
     */
    fun configure(directoryPath: String)

    /** True when the Japanese romanizer can run — every dictionary file present, or bundled. */
    fun isReady(): Boolean

    /**
     * Download, verify and install the dictionary. Success when [isReady] was already true.
     * Never throws — failures come back inside the [Result].
     */
    suspend fun download(): Result<Unit>
}
