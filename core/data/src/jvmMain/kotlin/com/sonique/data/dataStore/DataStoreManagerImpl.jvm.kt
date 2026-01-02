package com.sonique.data.dataStore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sonique.common.SETTINGS_FILENAME
import com.sonique.data.io.getHomeFolderPath
import createDataStore
import java.io.File

actual fun createDataStoreInstance(): DataStore<Preferences> = createDataStore(
    producePath = {
        val file = File(getHomeFolderPath(listOf(".com.sonique.com.sonique.app")), "$SETTINGS_FILENAME.preferences_pb")
        file.absolutePath
    }
)

