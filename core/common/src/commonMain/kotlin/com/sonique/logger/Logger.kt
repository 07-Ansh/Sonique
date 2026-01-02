package com.sonique.logger

import co.touchlab.kermit.Logger

object Logger {
    private val logger = Logger

    fun d(
        tag: String,
        message: String,
    ) {
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
        logger.i(tag, message = { message })
    }

    fun w(
        tag: String,
        message: String,
    ) {
        logger.w(tag, message = { message })
    }

    fun e(
        tag: String,
        message: String,
        e: Throwable? = null,
    ) {
        logger.e(tag, throwable = e, message = { message })
    }
}

enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

