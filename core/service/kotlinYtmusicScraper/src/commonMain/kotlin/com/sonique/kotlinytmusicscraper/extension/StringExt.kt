package com.sonique.kotlinytmusicscraper.extension

import okio.ByteString.Companion.encodeUtf8
import kotlin.random.Random

fun String.sha256(): String = encodeUtf8().sha256().hex()

fun randomString(length: Int): String {
    val chars = "0123456789abcdef"
    return (1..length)
        .map { chars[Random.nextInt(chars.length)] }
        .joinToString("")
}

fun String.verifyYouTubePlaylistId(): String = if (startsWith("VL")) this else "VL$this"

fun stripMarkdown(markdown: String): String =
    markdown
        .replace(Regex("""(?m)^#{1,6}\s*"""), "")
        .replace(Regex("""\*\*(.*?)\*\*"""), "$1")
        .replace(Regex("""\*(.*?)\*"""), "$1")
        .replace(Regex("""~~(.*?)~~"""), "$1")
        .replace(Regex("""`([^`]*)`"""), "$1")
        .replace(Regex("""!\[.*?\]\(.*?\)"""), "")
        .replace(Regex("""\[.*?\]\(.*?\)"""), "")
        .replace(Regex("""(?m)^\s*[-*_]{3,}\s*$"""), "\n")
        .replace(Regex("""(?m)^\s*[-*+]\s+"""), " - ")
        .replace(Regex("""(?m)^\s*\d+\.\s+"""), " - ")
        .replace(Regex("""\n{2,}"""), "\n\n")
        .lines()
        .joinToString("\n") { it.trim() }
