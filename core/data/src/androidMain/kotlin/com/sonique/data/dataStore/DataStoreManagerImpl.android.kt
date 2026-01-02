package com.sonique.data.dataStore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sonique.common.SETTINGS_FILENAME
import createDataStore
import org.koin.mp.KoinPlatform.getKoin

actual fun createDataStoreInstance(): DataStore<Preferences> {
    return createDataStore(
        producePath = { getKoin().get<Context>().filesDir.resolve("datastore/$SETTINGS_FILENAME.preferences_pb").absolutePath }
    )
}

