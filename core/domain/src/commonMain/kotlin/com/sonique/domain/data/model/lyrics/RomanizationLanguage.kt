package com.sonique.domain.data.model.lyrics

/**
 * The twelve languages whose lyrics can be shown in Latin script.
 *
 * The list is of LANGUAGES, not scripts, and the difference is why each entry exists separately:
 * seven of them are written in Cyrillic and share one alphabet while romanizing it differently —
 * Ukrainian `и` is `y` where Russian `и` is `i`; Macedonian keeps Serbian's `ј` but adds `ѓ`/`ќ`.
 * A single "Cyrillic" entry would be wrong for six of the seven.
 *
 * Lives in `domain` rather than beside the romanizers in `lyricsService`, because both sides need
 * it and they cannot see each other: `data` depends on `lyricsService` with `implementation`, so
 * the dependency stops there and never reaches `composeApp` — which is exactly the layering rule
 * working as intended. Anything the UI and a service must BOTH name belongs here.
 */
enum class RomanizationLanguage {
    JAPANESE,
    KOREAN,
    CHINESE,
    HINDI,
    PUNJABI,
    RUSSIAN,
    UKRAINIAN,
    SERBIAN,
    BULGARIAN,
    BELARUSIAN,
    KYRGYZ,
    MACEDONIAN,
    ;

    companion object {
        /**
         * Parses the stored preference — a comma-separated list of [name]s.
         *
         * Unknown entries are DROPPED rather than throwing: a preference file can outlive the
         * build that wrote it (a backup restored onto an older version, a language removed later),
         * and one stale token must not take the whole setting down with it.
         */
        fun parse(stored: String): Set<RomanizationLanguage> =
            stored
                .split(',')
                .mapNotNull { token ->
                    val trimmed = token.trim()
                    entries.firstOrNull { it.name == trimmed }
                }.toSet()
    }
}
