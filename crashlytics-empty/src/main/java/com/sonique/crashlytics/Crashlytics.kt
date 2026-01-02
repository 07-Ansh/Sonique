package com.sonique.crashlytics

import android.content.Context
import com.sonique.domain.data.player.PlayerError
import com.sonique.logger.Logger

// Sent crash to Sentry
fun reportCrash(throwable: Throwable) {
    Logger.e("Crashlytics", "NON-SENTRY crash: ${throwable.localizedMessage}")
}

fun configCrashlytics(applicationContext: Context, dsn: String) {
    Logger.d("Crashlytics", "NON-SENTRY start com.sonique.app")
}

fun pushPlayerError(error: PlayerError) {
    Logger.e("Crashlytics", "NON-SENTRY Player Error: ${error.message}, code: ${error.errorCode}, code name: ${error.errorCodeName}")
}

