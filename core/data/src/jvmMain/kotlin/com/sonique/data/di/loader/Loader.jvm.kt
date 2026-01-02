package com.sonique.data.di.loader

import com.sonique.media_jvm.di.loadGstreamerModule

actual fun loadMediaService() {
    loadGstreamerModule()
}

