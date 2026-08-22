package com.sonique.domain.data.model.lyrics

/**
 * Where the Japanese romanization dictionary stands on this device.
 *
 * Japanese is the one language whose romanizer needs a ~13 MB morphological dictionary, which is
 * no longer packaged inside the Android APK — it is fetched once, on demand, after the user turns
 * Japanese on. The other eleven languages never leave [READY] territory conceptually and do not
 * consult this state at all; platforms that still bundle the dictionary (Desktop) report [READY]
 * from the start.
 */
enum class RomanizationDictionaryState {
    /** No dictionary on disk yet — Japanese lines are left as they are, the existing null contract. */
    NOT_DOWNLOADED,

    /** A download is running right now. */
    DOWNLOADING,

    /** All dictionary files are present; the romanizer can build its analyzer. */
    READY,

    /** The last download attempt failed — selecting Japanese again retries. */
    FAILED,
}
