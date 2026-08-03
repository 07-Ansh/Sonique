package com.sonique.kotlinytmusicscraper.utils

import okio.ByteString.Companion.encodeUtf8

fun sha1(str: String): String = str.encodeUtf8().sha1().hex()

fun parseCookieString(cookie: String): Map<String, String> =
    cookie
        .split("; ")
        .filter { it.isNotEmpty() }
        .associate {
            val (key, value) = it.split("=")
            key to value
        }

fun String.parseTime(): Int? {
    try {
        val parts =
            if (this.contains(":")) split(":").map { it.toInt() } else split(".").map { it.toInt() }
        if (parts.size == 2) {
            return parts[0] * 60 + parts[1]
        }
        if (parts.size == 3) {
            return parts[0] * 3600 + parts[1] * 60 + parts[2]
        }
    } catch (e: Exception) {
        return null
    }
    return null
}

fun generateNetscapeCookies(
    cookies: Map<String, String>,
    domain: String = ".example.com",
    path: String = "/",
    secure: Boolean = false,
    httpOnly: Boolean = false,
    expirationTimeSeconds: Long? = null,
): String {
    val expTime = expirationTimeSeconds ?: 2147483647L
    val header =
        "# Netscape HTTP Cookie File\n" +
            "# This is a generated file! Do not edit.\n\n"

    val cookieLines =
        cookies
            .map { (name, value) ->
                buildString {
                    append(domain)
                    append("\t")
                    append("TRUE")
                    append("\t")
                    append(path)
                    append("\t")
                    append(if (secure) "TRUE" else "FALSE")
                    append("\t")
                    append(expTime)
                    append("\t")
                    append(name)
                    append("\t")
                    append(value)
                }
            }.joinToString("\n")

    return header + cookieLines
}
