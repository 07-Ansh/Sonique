package com.sonique.app.expect

import com.sonique.app.BuildConfig

actual fun isDebugBuild(): Boolean = BuildConfig.DEBUG
