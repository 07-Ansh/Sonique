package com.sonique.lyrics.romanization

import com.atilika.kuromoji.dict.CharacterDefinitions
import com.atilika.kuromoji.dict.ConnectionCosts
import com.atilika.kuromoji.dict.InsertedDictionary
import com.atilika.kuromoji.dict.TokenInfoDictionary
import com.atilika.kuromoji.dict.UnknownDictionary
import com.atilika.kuromoji.ipadic.Tokenizer
import com.atilika.kuromoji.trie.DoubleArrayTrie
import com.atilika.kuromoji.util.ResourceResolver
import com.sonique.ktorext.getEngine
import com.sonique.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "KuromojiDictionary"

/**
 * Everything Android knows about the kuromoji ipadic dictionary living OUTSIDE the APK.
 *
 * The eight `.bin` resources under `com/atilika/kuromoji/ipadic/` are ~13 MB — excluded from the
 * APK (`androidApp/build.gradle.kts` packaging block) and fetched once, on demand, into a plain
 * directory under `filesDir`. kuromoji is then pointed at that directory through the one seam its
 * API leaves open: `TokenizerBase.Builder`'s protected `resolver` field.
 *
 * The subtlety is WHERE the resolver must be planted. `com.atilika.kuromoji.ipadic.
 * Tokenizer.Builder.loadDictionaries()` assigns `resolver = new SimpleResourceResolver(
 * this.getClass())` unconditionally at its own top, so a resolver assigned in a subclass `init`
 * block is overwritten before the first byte is read — the override has to be of
 * `loadDictionaries()` itself, restating its short body (verified against the 0.9.0 sources jar;
 * kuromoji's last release, 2015, so the body is not going to drift).
 */
internal object KuromojiDictionary {
    /** The one place the pack's identity lives: URL, digest and expected size, together. */
    private const val DOWNLOAD_URL =
        "https://github.com/maxrave-dev/simpmusic-files/releases/download/abc/kuromoji-ipadic-0.9.0-dict.tar.gz"
    private const val ARCHIVE_SHA_256 = "ea18a64ff57e574bd20b3e21c20d16591308796b608c01fc39c5ef9ef8b2c761"
    private const val ARCHIVE_SIZE_BYTES = 13_329_435L

    private const val TAR_BLOCK_BYTES = 512
    private const val COPY_BUFFER_BYTES = 64 * 1024

    /**
     * The eight resources kuromoji 0.9.0 asks its resolver for — it passes exactly these BARE
     * names (`DoubleArrayTrie.DOUBLE_ARRAY_TRIE_FILENAME` and friends), no package path, though
     * [DirectoryResourceResolver] strips one anyway in case a future version qualifies them.
     * "Ready" means all eight are on disk, nothing less.
     */
    private val DICTIONARY_FILE_NAMES =
        setOf(
            "characterDefinitions.bin",
            "connectionCosts.bin",
            "doubleArrayTrie.bin",
            "tokenInfoDictionary.bin",
            "tokenInfoFeaturesMap.bin",
            "tokenInfoPartOfSpeechMap.bin",
            "tokenInfoTargetMap.bin",
            "unknownDictionary.bin",
        )

    /** All eight files present and non-empty. `File.length()` is 0 for a missing file, so one call covers both. */
    fun isReady(directory: File): Boolean = DICTIONARY_FILE_NAMES.all { name -> File(directory, name).length() > 0 }

    /** Builds the analyzer from an installed directory. Throws if the files are unreadable — callers catch. */
    fun buildTokenizer(directory: File): Tokenizer = DirectoryDictionaryBuilder(directory).build()

    /**
     * Fetch the pack, verify its SHA-256, unpack the eight files and move them into [directory]
     * in one rename — so [isReady] can never observe a half-installed dictionary. Every failure
     * cleans up after itself and comes back as a [Result]; already-installed is success.
     */
    suspend fun download(directory: File): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (isReady(directory)) return@withContext Result.success(Unit)
            val parent =
                directory.parentFile
                    ?: return@withContext Result.failure(IOException("dictionary directory $directory has no parent"))
            if (!parent.isDirectory && !parent.mkdirs()) {
                return@withContext Result.failure(IOException("could not create $parent"))
            }
            // Staged beside the final directory (same filesystem), so the last step is a plain
            // atomic rename rather than a copy that can die halfway.
            val archive = File(parent, "${directory.name}.tar.gz.part")
            val staging = File(parent, "${directory.name}.staging")
            try {
                staging.deleteRecursively()
                fetchArchive(archive)
                extractDictionary(archive, staging)
                val missing = DICTIONARY_FILE_NAMES.filterNot { name -> File(staging, name).length() > 0 }
                if (missing.isNotEmpty()) {
                    throw IOException("dictionary pack is missing ${missing.joinToString()}")
                }
                if (directory.exists() && !directory.deleteRecursively()) {
                    throw IOException("could not clear the old $directory")
                }
                if (!staging.renameTo(directory)) {
                    throw IOException("could not move the staged dictionary into $directory")
                }
                Logger.w(TAG, "Japanese dictionary installed into $directory")
                Result.success(Unit)
            } catch (cancellation: CancellationException) {
                // Cancellation is the caller's business, never a FAILED state — but the partial
                // staging must not be left behind to fool a later isReady-by-hand inspection.
                staging.deleteRecursively()
                throw cancellation
            } catch (failure: Exception) {
                Logger.e(TAG, "Japanese dictionary download failed: ${failure.message}")
                staging.deleteRecursively()
                Result.failure(failure)
            } finally {
                archive.delete()
            }
        }

    /** Streams the release asset to [into], hashing as it goes — the 13 MB is never held in memory. */
    private suspend fun fetchArchive(into: File) {
        val client =
            HttpClient(getEngine()) {
                // Same shape as AutoEq's client: statuses are handled by hand, and GitHub answers
                // release-asset URLs with a redirect to its object store, so redirects must follow.
                expectSuccess = false
                followRedirects = true
            }
        try {
            client.prepareGet(DOWNLOAD_URL).execute { response ->
                if (!response.status.isSuccess()) {
                    throw IOException("HTTP ${response.status.value} fetching the dictionary pack")
                }
                val declaredLength = response.contentLength()
                if (declaredLength != null && declaredLength != ARCHIVE_SIZE_BYTES) {
                    // Cheap early abort before 13 MB of traffic: the asset at this URL is pinned,
                    // so a different size IS the wrong file and the digest check would only say
                    // the same thing later.
                    throw IOException("dictionary pack is $declaredLength bytes, expected $ARCHIVE_SIZE_BYTES")
                }
                val digest = MessageDigest.getInstance("SHA-256")
                val channel = response.bodyAsChannel()
                into.outputStream().buffered().use { out ->
                    val buffer = ByteArray(COPY_BUFFER_BYTES)
                    while (true) {
                        val count = channel.readAvailable(buffer, 0, buffer.size)
                        if (count == -1) break
                        if (count == 0) continue
                        digest.update(buffer, 0, count)
                        out.write(buffer, 0, count)
                    }
                }
                val actual = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
                if (!actual.equals(ARCHIVE_SHA_256, ignoreCase = true)) {
                    throw IOException("dictionary pack checksum mismatch: expected $ARCHIVE_SHA_256, got $actual")
                }
            }
        } finally {
            client.close()
        }
    }

    /**
     * A deliberately minimal tar reader for a flat archive this project publishes itself: 512-byte
     * headers, octal sizes, entries taken ONLY when their basename is one of the eight dictionary
     * files. That whitelist is also the defence — pax headers, directories, macOS `._` sidecars
     * and anything with a path in its name are skipped, so no entry can ever write outside [into].
     */
    private fun extractDictionary(
        archive: File,
        into: File,
    ) {
        if (!into.mkdirs() && !into.isDirectory) throw IOException("could not create $into")
        GZIPInputStream(archive.inputStream().buffered()).use { tar ->
            val header = ByteArray(TAR_BLOCK_BYTES)
            while (readTarBlock(tar, header)) {
                // Two all-zero blocks end an archive; one is enough to stop reading.
                if (header.all { it == 0.toByte() }) break
                val entryName = tarString(header, offset = 0, length = 100)
                val entrySize = tarOctal(header, offset = 124, length = 12)
                val typeFlag = header[156]
                // '\0' is the pre-POSIX spelling of '0' (regular file); everything else — pax
                // metadata ('x'/'g'), directories ('5'), links — is skipped over by size.
                val isRegularFile = typeFlag == 0.toByte() || typeFlag == '0'.code.toByte()
                val fileName = entryName.substringAfterLast('/')
                if (isRegularFile && fileName in DICTIONARY_FILE_NAMES) {
                    copyExactly(tar, File(into, fileName), entrySize, entryName)
                } else {
                    skipExactly(tar, entrySize)
                }
                skipExactly(tar, (TAR_BLOCK_BYTES - (entrySize % TAR_BLOCK_BYTES)) % TAR_BLOCK_BYTES)
            }
        }
    }

    /** One 512-byte header. False on clean EOF at a block boundary, IOException mid-block. */
    private fun readTarBlock(
        input: InputStream,
        block: ByteArray,
    ): Boolean {
        var offset = 0
        while (offset < block.size) {
            val count = input.read(block, offset, block.size - offset)
            if (count == -1) {
                if (offset == 0) return false
                throw IOException("truncated tar header")
            }
            offset += count
        }
        return true
    }

    private fun copyExactly(
        input: InputStream,
        target: File,
        byteCount: Long,
        entryName: String,
    ) {
        target.outputStream().buffered().use { out ->
            val buffer = ByteArray(COPY_BUFFER_BYTES)
            var remaining = byteCount
            while (remaining > 0) {
                val count = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                if (count == -1) throw IOException("archive ended inside $entryName")
                out.write(buffer, 0, count)
                remaining -= count
            }
        }
    }

    private fun skipExactly(
        input: InputStream,
        byteCount: Long,
    ) {
        // read() rather than skip(): GZIPInputStream's skip is allowed to stop short without
        // being at EOF, and the retry loop it forces is longer than just reading into scratch.
        val scratch = ByteArray(COPY_BUFFER_BYTES)
        var remaining = byteCount
        while (remaining > 0) {
            val count = input.read(scratch, 0, minOf(scratch.size.toLong(), remaining).toInt())
            if (count == -1) throw IOException("truncated tar entry")
            remaining -= count
        }
    }

    /** Bytes up to the first NUL. Tar text fields are NUL-terminated inside a fixed-width slot. */
    private fun tarString(
        block: ByteArray,
        offset: Int,
        length: Int,
    ): String {
        var end = offset
        val limit = offset + length
        while (end < limit && block[end] != 0.toByte()) end++
        return String(block, offset, end - offset, Charsets.UTF_8)
    }

    /** Octal-in-ASCII, the only size encoding a 13 MB flat archive can carry (base-256 starts at 8 GB). */
    private fun tarOctal(
        block: ByteArray,
        offset: Int,
        length: Int,
    ): Long {
        val text = tarString(block, offset, length).trim { it == ' ' || it == '\u0000' }
        if (text.isEmpty()) return 0L
        return text.toLongOrNull(radix = 8) ?: throw IOException("bad size field in tar header: \"$text\"")
    }
}

/** `name -> File(directory, basename(name))`: serves kuromoji's bare names and qualified ones alike. */
private class DirectoryResourceResolver(
    private val directory: File,
) : ResourceResolver {
    override fun resolve(resourceName: String): InputStream {
        val file = File(directory, resourceName.substringAfterLast('/'))
        if (file.length() <= 0L) throw IOException("dictionary file not found: $file")
        return file.inputStream().buffered()
    }
}

/**
 * `Tokenizer.Builder` that loads from a directory instead of the classpath.
 *
 * The body restates `Tokenizer.Builder.loadDictionaries()` (0.9.0) because that method plants its
 * own classpath resolver as its first act — there is no seam to inject one earlier. The penalties
 * list restates the parent's defaults `[2, 3000, 7, 1700]` for the same reason: the fields behind
 * them are private there. Only NORMAL mode is ever built here, which does not read them, but they
 * are kept identical so a future SEARCH-mode caller inherits upstream behaviour, not a surprise.
 */
private class DirectoryDictionaryBuilder(
    private val directory: File,
) : Tokenizer.Builder() {
    override fun loadDictionaries() {
        penalties = listOf(2, 3000, 7, 1700)
        resolver = DirectoryResourceResolver(directory)
        try {
            doubleArrayTrie = DoubleArrayTrie.newInstance(resolver)
            connectionCosts = ConnectionCosts.newInstance(resolver)
            tokenInfoDictionary = TokenInfoDictionary.newInstance(resolver)
            characterDefinitions = CharacterDefinitions.newInstance(resolver)
            unknownDictionary = UnknownDictionary.newInstance(resolver, characterDefinitions, totalFeatures)
            insertedDictionary = InsertedDictionary(totalFeatures)
        } catch (loadFailure: Exception) {
            throw RuntimeException("Could not load dictionaries from $directory", loadFailure)
        }
    }
}
