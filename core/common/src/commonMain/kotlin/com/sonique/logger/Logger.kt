package com.sonique.logger

import co.touchlab.kermit.Logger

object Logger {
    private val logger = Logger
    private const val isDebug = false

    fun d(
        tag: String,
        message: String,
    ) {
        if (!isDebug) return
        logger.d(
            tag,
            message = {
                message
            }
        )
    }

    fun i(
        tag: String,
        message: String,
    ) {
        if (!isDebug) return
        logger.i(tag, message = { message })
    }

    fun w(
        tag: String,
        message: String,
    ) {
        if (!isDebug) return
        logger.w(tag, message = { message })
    }

    fun e(
        tag: String,
        message: String,
        e: Throwable? = null,
    ) {
        if (!isDebug) return
        logger.e(tag, throwable = e, message = { message })
    }
}

enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

