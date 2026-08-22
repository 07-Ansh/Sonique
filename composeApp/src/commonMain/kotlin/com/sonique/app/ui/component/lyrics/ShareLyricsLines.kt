package com.sonique.app.ui.component.lyrics

import com.sonique.app.ui.component.stripRichSyncTimestamps
import com.sonique.app.viewModel.NowPlayingScreenData

internal fun NowPlayingScreenData.LyricsData.toShareLyricsLines(): List<String> =
    lyrics.lines
        ?.map { it.words.stripRichSyncTimestamps() }
        .orEmpty()
