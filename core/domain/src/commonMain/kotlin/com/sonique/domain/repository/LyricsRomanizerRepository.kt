package com.sonique.domain.repository

import com.sonique.domain.data.model.lyrics.RomanizationDictionaryState
import com.sonique.domain.data.model.lyrics.RomanizationLanguage
import kotlinx.coroutines.flow.StateFlow

interface LyricsRomanizerRepository {
    fun romanize(
        line: String,
        enabled: Set<RomanizationLanguage>,
    ): String?

    val japaneseDictionaryState: StateFlow<RomanizationDictionaryState>

    suspend fun downloadJapaneseDictionary()
}