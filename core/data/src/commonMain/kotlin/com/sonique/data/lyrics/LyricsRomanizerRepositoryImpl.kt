package com.sonique.data.lyrics

import com.sonique.domain.data.model.lyrics.RomanizationDictionaryState
import com.sonique.domain.data.model.lyrics.RomanizationLanguage
import com.sonique.domain.repository.LyricsRomanizerRepository
import com.sonique.lyrics.romanization.LyricsRomanizer
import com.sonique.lyrics.romanization.RomanizationDictionaryPack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

class LyricsRomanizerRepositoryImpl(
    japaneseDictionaryDirectoryPath: String,
) : LyricsRomanizerRepository {
    init {
        RomanizationDictionaryPack.configure(japaneseDictionaryDirectoryPath)
    }

    private val _japaneseDictionaryState =
        MutableStateFlow(
            if (RomanizationDictionaryPack.isReady()) {
                RomanizationDictionaryState.READY
            } else {
                RomanizationDictionaryState.NOT_DOWNLOADED
            },
        )
    override val japaneseDictionaryState: StateFlow<RomanizationDictionaryState> =
        _japaneseDictionaryState.asStateFlow()

    private val downloadMutex = Mutex()

    override suspend fun downloadJapaneseDictionary() {
        downloadMutex.withLock {
            if (_japaneseDictionaryState.value == RomanizationDictionaryState.READY) return
            _japaneseDictionaryState.value = RomanizationDictionaryState.DOWNLOADING
            try {
                val result = RomanizationDictionaryPack.download()
                _japaneseDictionaryState.value =
                    if (result.isSuccess) {
                        RomanizationDictionaryState.READY
                    } else {
                        RomanizationDictionaryState.FAILED
                    }
            } catch (cancellation: CancellationException) {
                _japaneseDictionaryState.value = RomanizationDictionaryState.NOT_DOWNLOADED
                throw cancellation
            }
        }
    }

    override fun romanize(
        line: String,
        enabled: Set<RomanizationLanguage>,
    ): String? = LyricsRomanizer.romanize(line, enabled)
}