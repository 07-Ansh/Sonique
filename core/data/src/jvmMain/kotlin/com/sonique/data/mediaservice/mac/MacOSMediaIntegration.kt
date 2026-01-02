package com.sonique.data.mediaservice.mac

import com.sonique.logger.Logger
import com.sun.jna.Callback
import com.sun.jna.CallbackReference
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Platform
import com.sun.jna.Pointer
import com.sun.jna.Structure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "MacOSMediaIntegration"

/**
 * MPNowPlayingPlaybackState constants
 */
object MPNowPlayingPlaybackState {
    const val Unknown = 0
    const val Playing = 1
    const val Paused = 2
    const val Stopped = 3
    const val Interrupted = 4
}

/**
 * MPRemoteCommandHandlerStatus constants
 */
object MPRemoteCommandHandlerStatus {
    const val Success = 0
    const val NoSuchContent = 1
    const val NoActionableNowPlayingItem = 2
    const val DeviceNotFound = 3
    const val CommandFailed = 4
}

/**
 * Now Playing info keys - These are the actual string values of the constants
 * Found by inspecting the MediaPlayer framework symbols
 */
object MPMediaItemProperty {
    // These are the actual runtime values of the NSString constants
    const val Title = "title"
    const val Artist = "artist"
    const val Album = "albumTitle"
    const val PlaybackDuration = "playbackDuration"
    const val Artwork = "artwork"
}

object MPNowPlayingInfoProperty {
    // These constants use their full names as values
    const val ElapsedPlaybackTime = "MPNowPlayingInfoPropertyElapsedPlaybackTime"
    const val PlaybackRate = "MPNowPlayingInfoPropertyPlaybackRate"
    const val DefaultPlaybackRate = "MPNowPlayingInfoPropertyDefaultPlaybackRate"
    const val PlaybackQueueIndex = "MPNowPlayingInfoPropertyPlaybackQueueIndex"
    const val PlaybackQueueCount = "MPNowPlayingInfoPropertyPlaybackQueueCount"
    const val MediaType = "MPNowPlayingInfoPropertyMediaType"
}

/**
 * Helper object to get MediaPlayer framework constants
 * The constants are NSString* symbols exported from the framework
 */
object MPConstants {
    private var loaded = false
    private val constants = mutableMapOf<String, Pointer?>()

    private fun ensureLoaded() {
        if (loaded) return
        try {
            // Load MediaPlayer framework to access its exported symbols
            val lib =
                com.sun.jna.NativeLibrary
                    .getInstance("MediaPlayer")

            // Get the actual NSString* constant values from the framework
            val symbolNames =
                listOf(
                    "MPMediaItemPropertyTitle",
                    "MPMediaItemPropertyArtist",
                    "MPMediaItemPropertyAlbumTitle",
                    "MPMediaItemPropertyPlaybackDuration",
                    "MPMediaItemPropertyArtwork",
                    "MPNowPlayingInfoPropertyElapsedPlaybackTime",
                    "MPNowPlayingInfoPropertyPlaybackRate",
                    "MPNowPlayingInfoPropertyDefaultPlaybackRate",
                    "MPNowPlayingInfoPropertyPlaybackQueueIndex",
                    "MPNowPlayingInfoPropertyPlaybackQueueCount",
                    "MPNowPlayingInfoPropertyMediaType",
                )

            for (name in symbolNames) {
                try {
                    val symbolPtr = lib.getGlobalVariableAddress(name)
                    // The symbol is a pointer to NSString*, so we need to dereference it
                    val nsStringPtr = symbolPtr.getPointer(0)
                    constants[name] = nsStringPtr
                    Logger.d(TAG, "Loaded constant $name: $nsStringPtr")
                } catch (e: Exception) {
                    Logger.w(TAG, "Could not load constant $name: ${e.message}")
                }
            }
            loaded = true
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to load MediaPlayer constants: ${e.message}", e)
        }
    }

    fun get(name: String): Pointer? {
        ensureLoaded()
        return constants[name]
    }

    // Convenience methods
    val title: Pointer? get() = get("MPMediaItemPropertyTitle")
    val artist: Pointer? get() = get("MPMediaItemPropertyArtist")
    val album: Pointer? get() = get("MPMediaItemPropertyAlbumTitle")
    val playbackDuration: Pointer? get() = get("MPMediaItemPropertyPlaybackDuration")
    val artwork: Pointer? get() = get("MPMediaItemPropertyArtwork")
    val elapsedPlaybackTime: Pointer? get() = get("MPNowPlayingInfoPropertyElapsedPlaybackTime")
    val playbackRate: Pointer? get() = get("MPNowPlayingInfoPropertyPlaybackRate")
    val defaultPlaybackRate: Pointer? get() = get("MPNowPlayingInfoPropertyDefaultPlaybackRate")
    val playbackQueueIndex: Pointer? get() = get("MPNowPlayingInfoPropertyPlaybackQueueIndex")
    val playbackQueueCount: Pointer? get() = get("MPNowPlayingInfoPropertyPlaybackQueueCount")
    val mediaType: Pointer? get() = get("MPNowPlayingInfoPropertyMediaType")
}

/**
 * Listener interface for remote commands from macOS
 */
interface MacOSRemoteCommandListener {
    fun onPlay()

    fun onPause()

    fun onTogglePlayPause()

    fun onStop()

    fun onNextTrack()

    fun onPreviousTrack()

    fun onSeekForward()

    fun onSeekBackward()

    fun onChangePlaybackPosition(positionSeconds: Double)
}

/**
 * Data class for Now Playing info
 */
data class NowPlayingInfo(
    val title: String,
    val artist: String,
    val album: String = "",
    val durationSeconds: Double,
    val elapsedTimeSeconds: Double = 0.0,
    val playbackRate: Double = 1.0,
    val artworkUrl: String? = null,
    val queueIndex: Int = 0,
    val queueCount: Int = 1,
)

/**
 * Block descriptor structure for Objective-C blocks
 */
@Structure.FieldOrder("reserved", "size", "copy_helper", "dispose_helper", "signature")
class BlockDescriptor : Structure() {
    @JvmField
    var reserved: Long = 0

    @JvmField
    var size: Long = 0

    @JvmField
    var copy_helper: Pointer? = null

    @JvmField
    var dispose_helper: Pointer? = null

    @JvmField
    var signature: Pointer? = null
}

/**
 * Block literal structure for Objective-C blocks
 * This is a simplified version - in production you might need more fields
 */
@Structure.FieldOrder("isa", "flags", "reserved", "invoke", "descriptor")
class BlockLiteral : Structure() {
    @JvmField
    var isa: Pointer? = null

    @JvmField
    var flags: Int = 0

    @JvmField
    var reserved: Int = 0

    @JvmField
    var invoke: Pointer? = null

    @JvmField
    var descriptor: Pointer? = null

    companion object {
        // Block flags
        const val BLOCK_HAS_COPY_DISPOSE = (1 shl 25)
        const val BLOCK_HAS_CTOR = (1 shl 26)
        const val BLOCK_IS_GLOBAL = (1 shl 28)
        const val BLOCK_HAS_STRET = (1 shl 29)
        const val BLOCK_HAS_SIGNATURE = (1 shl 30)
    }
}

/**
 * JNA interface for loading dynamic libraries
 */
private interface DynamicLibrary : Library {
    companion object {
        fun load(name: String): DynamicLibrary? =
            try {
                Native.load(name, DynamicLibrary::class.java)
            } catch (e: Exception) {
                null
            }
    }
}

/**
 * Main class for macOS Media Integration
 * Handles Now Playing Center and Remote Command Center
 */
class MacOSMediaIntegration private constructor() {
    private val initialized = AtomicBoolean(false)
    private var remoteCommandListener: MacOSRemoteCommandListener? = null

    // Keep references to prevent garbage collection
    private val blockCallbacks = mutableListOf<Callback>()
    private val blockMemories = mutableListOf<Memory>()

    // Cache for artwork images to avoid re-downloading
    private val artworkCache = ConcurrentHashMap<String, Pointer>()
    private var currentArtworkUrl: String? = null

    companion object {
        @Volatile
        private var instance: MacOSMediaIntegration? = null

        @Volatile
        private var frameworkLoaded = false

        fun getInstance(): MacOSMediaIntegration =
            instance ?: synchronized(this) {
                instance ?: MacOSMediaIntegration().also { instance = it }
            }

        /**
         * Check if running on macOS
         */
        fun isSupported(): Boolean = Platform.isMac()

        /**
         * Load the MediaPlayer framework
         * This must be called before using any MediaPlayer APIs
         */
        private fun loadMediaPlayerFramework(): Boolean {
            if (frameworkLoaded) return true

            return try {
                // Load the MediaPlayer framework
                // On macOS, frameworks are located at /System/Library/Frameworks/
                val frameworkPath = "/System/Library/Frameworks/MediaPlayer.framework/MediaPlayer"
                System.load(frameworkPath)
                frameworkLoaded = true
                Logger.d(TAG, "MediaPlayer framework loaded successfully")
                true
            } catch (e: UnsatisfiedLinkError) {
                // Try alternative approach - just reference the framework classes
                // They may be loaded on-demand by the Objective-C runtime
                Logger.w(TAG, "Could not load MediaPlayer framework directly: ${e.message}")
                // Check if classes are available
                val cls = ObjC.cls("MPNowPlayingInfoCenter")
                if (cls != null) {
                    frameworkLoaded = true
                    Logger.d(TAG, "MediaPlayer classes available via runtime")
                    true
                } else {
                    Logger.e(TAG, "MediaPlayer framework not available")
                    false
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Failed to load MediaPlayer framework: ${e.message}", e)
                false
            }
        }
    }

    /**
     * Initialize the media integration
     * Must be called from the main thread
     */
    fun initialize(): Boolean {
        if (!isSupported()) {
            Logger.w(TAG, "MacOS media integration not supported on this platform")
            return false
        }

        if (initialized.get()) {
            Logger.d(TAG, "Already initialized")
            return true
        }

        try {
            // Load MediaPlayer framework first
            if (!loadMediaPlayerFramework()) {
                Logger.e(TAG, "Failed to load MediaPlayer framework")
                return false
            }

            // Get the shared MPRemoteCommandCenter
            val commandCenter = getRemoteCommandCenter()
            if (commandCenter == null) {
                Logger.e(TAG, "Failed to get MPRemoteCommandCenter")
                return false
            }

            // Setup remote commands
            setupRemoteCommands(commandCenter)

            initialized.set(true)
            Logger.d(TAG, "MacOS media integration initialized successfully")
            return true
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to initialize MacOS media integration: ${e.message}", e)
            return false
        }
    }

    /**
     * Set the listener for remote commands
     */
    fun setRemoteCommandListener(listener: MacOSRemoteCommandListener?) {
        this.remoteCommandListener = listener
    }

    /**
     * Update Now Playing info
     */
    fun updateNowPlayingInfo(info: NowPlayingInfo) {
        if (!isSupported() || !initialized.get()) {
            Logger.w(TAG, "updateNowPlayingInfo: Not supported or not initialized")
            return
        }

        try {
            val infoCenter = getNowPlayingInfoCenter()
            if (infoCenter == null) {
                Logger.e(TAG, "Failed to get NowPlayingInfoCenter")
                return
            }

            val dict = ObjC.nsMutableDictionary()
            if (dict == null) {
                Logger.e(TAG, "Failed to create NSMutableDictionary")
                return
            }

            Logger.d(TAG, "Creating now playing info for: ${info.title} - ${info.artist}")

            // Use the actual framework constants for keys
            // Set title
            val titleKey = MPConstants.title
            val titleValue = ObjC.nsString(info.title)
            if (titleKey != null && titleValue != null) {
                ObjC.dictionarySetObject(dict, titleValue, titleKey)
                Logger.d(TAG, "Set title: ${info.title} (key: $titleKey)")
            } else {
                Logger.e(TAG, "Failed to set title - key: $titleKey, value: $titleValue")
            }

            // Set artist
            val artistKey = MPConstants.artist
            val artistValue = ObjC.nsString(info.artist)
            if (artistKey != null && artistValue != null) {
                ObjC.dictionarySetObject(dict, artistValue, artistKey)
                Logger.d(TAG, "Set artist: ${info.artist}")
            } else {
                Logger.e(TAG, "Failed to set artist - key: $artistKey")
            }

            // Set album
            if (info.album.isNotEmpty()) {
                val albumKey = MPConstants.album
                val albumValue = ObjC.nsString(info.album)
                if (albumKey != null && albumValue != null) {
                    ObjC.dictionarySetObject(dict, albumValue, albumKey)
                }
            }

            // Set duration
            val durationKey = MPConstants.playbackDuration
            val durationValue = ObjC.nsNumber(info.durationSeconds)
            if (durationKey != null && durationValue != null) {
                ObjC.dictionarySetObject(dict, durationValue, durationKey)
                Logger.d(TAG, "Set duration: ${info.durationSeconds}")
            } else {
                Logger.e(TAG, "Failed to set duration - key: $durationKey, value: $durationValue")
            }

            // Set elapsed time
            val elapsedKey = MPConstants.elapsedPlaybackTime
            val elapsedValue = ObjC.nsNumber(info.elapsedTimeSeconds)
            if (elapsedKey != null && elapsedValue != null) {
                ObjC.dictionarySetObject(dict, elapsedValue, elapsedKey)
            } else {
                Logger.w(TAG, "Failed to set elapsed time - key: $elapsedKey")
            }

            // Set playback rate
            val rateKey = MPConstants.playbackRate
            val rateValue = ObjC.nsNumber(info.playbackRate)
            if (rateKey != null && rateValue != null) {
                ObjC.dictionarySetObject(dict, rateValue, rateKey)
            }

            // Set default playback rate
            val defaultRateKey = MPConstants.defaultPlaybackRate
            val defaultRateValue = ObjC.nsNumber(1.0)
            if (defaultRateKey != null && defaultRateValue != null) {
                ObjC.dictionarySetObject(dict, defaultRateValue, defaultRateKey)
            }

            // Set queue info
            val queueIndexKey = MPConstants.playbackQueueIndex
            val queueIndexValue = ObjC.nsNumber(info.queueIndex)
            if (queueIndexKey != null && queueIndexValue != null) {
                ObjC.dictionarySetObject(dict, queueIndexValue, queueIndexKey)
            }

            val queueCountKey = MPConstants.playbackQueueCount
            val queueCountValue = ObjC.nsNumber(info.queueCount)
            if (queueCountKey != null && queueCountValue != null) {
                ObjC.dictionarySetObject(dict, queueCountValue, queueCountKey)
            }

            // Set media type (1 = Music)
            val mediaTypeKey = MPConstants.mediaType
            val mediaTypeValue = ObjC.nsNumber(1)
            if (mediaTypeKey != null && mediaTypeValue != null) {
                ObjC.dictionarySetObject(dict, mediaTypeValue, mediaTypeKey)
            }

            // Log dictionary count for verification
            val countSel = ObjC.sel("count")
            if (countSel != null) {
                val count = ObjCRuntime.INSTANCE.objc_msgSend(dict, countSel)
                Logger.d(TAG, "Dictionary has entries (pointer: $count)")
            }

            // Set the now playing info
            ObjC.msg(infoCenter, "setNowPlayingInfo:", dict)

            Logger.d(TAG, "Updated now playing info: ${info.title} - ${info.artist}")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to update now playing info: ${e.message}", e)
        }
    }

    /**
     * Update playback state
     */
    fun updatePlaybackState(isPlaying: Boolean) {
        if (!isSupported() || !initialized.get()) return

        try {
            val infoCenter = getNowPlayingInfoCenter() ?: return
            val state = if (isPlaying) MPNowPlayingPlaybackState.Playing else MPNowPlayingPlaybackState.Paused
            ObjC.msg(infoCenter, "setPlaybackState:", state)
            Logger.d(TAG, "Updated playback state: ${if (isPlaying) "Playing" else "Paused"}")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to update playback state: ${e.message}", e)
        }
    }

    /**
     * Update the enabled state of remote command buttons
     * @param hasNext Whether there is a next track available
     * @param hasPrevious Whether there is a previous track available
     * @param canSeek Whether seeking is supported
     */
    fun updateCommandsEnabled(
        hasNext: Boolean,
        hasPrevious: Boolean,
        canSeek: Boolean = true,
    ) {
        if (!isSupported() || !initialized.get()) return

        try {
            val commandCenter = getRemoteCommandCenter() ?: return

            // Note: Objective-C BOOL is actually a signed char (1 byte)
            // We use Byte (1 or 0) instead of Boolean for proper JNA mapping
            val nextEnabled: Byte = if (hasNext) 1 else 0
            val prevEnabled: Byte = if (hasPrevious) 1 else 0
            val seekEnabled: Byte = if (canSeek) 1 else 0

            // Update next track command
            val nextCommand = ObjC.msg(commandCenter, "nextTrackCommand")
            if (nextCommand != null) {
                setCommandEnabled(nextCommand, nextEnabled)
                Logger.d(TAG, "Next command enabled: $hasNext")
            }

            // Update previous track command
            val prevCommand = ObjC.msg(commandCenter, "previousTrackCommand")
            if (prevCommand != null) {
                setCommandEnabled(prevCommand, prevEnabled)
                Logger.d(TAG, "Previous command enabled: $hasPrevious")
            }

            // Update seek commands
            val seekForwardCommand = ObjC.msg(commandCenter, "seekForwardCommand")
            if (seekForwardCommand != null) {
                setCommandEnabled(seekForwardCommand, seekEnabled)
            }

            val seekBackwardCommand = ObjC.msg(commandCenter, "seekBackwardCommand")
            if (seekBackwardCommand != null) {
                setCommandEnabled(seekBackwardCommand, seekEnabled)
            }

            val changePositionCommand = ObjC.msg(commandCenter, "changePlaybackPositionCommand")
            if (changePositionCommand != null) {
                setCommandEnabled(changePositionCommand, seekEnabled)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to update commands enabled state: ${e.message}", e)
        }
    }

    /**
     * Set enabled state on a command using proper Objective-C BOOL type
     * Note: On ARM64 and x86_64, BOOL is passed as int in objc_msgSend
     */
    private fun setCommandEnabled(
        command: Pointer,
        enabled: Byte,
    ) {
        // Use int for BOOL - on modern macOS/ARM64, BOOL is promoted to int in variadic functions
        val enabledInt = enabled.toInt()
        ObjCRuntime.INSTANCE.objc_msgSend(command, ObjC.sel("setEnabled:"), enabledInt)
    }

    /**
     * Update elapsed time (for seek bar progress)
     */
    fun updateElapsedTime(
        elapsedSeconds: Double,
        playbackRate: Double = 1.0,
    ) {
        if (!isSupported() || !initialized.get()) return

        try {
            val infoCenter = getNowPlayingInfoCenter() ?: return

            // Get current now playing info
            val currentInfo = ObjC.msg(infoCenter, "nowPlayingInfo")
            if (currentInfo == null) {
                Logger.w(TAG, "No current now playing info to update")
                return
            }

            // Create mutable copy
            val mutableInfo = ObjC.msg(currentInfo, "mutableCopy") ?: return

            // Update elapsed time using framework constant
            val elapsedKey = MPConstants.elapsedPlaybackTime
            val elapsedValue = ObjC.nsNumber(elapsedSeconds)
            if (elapsedKey != null && elapsedValue != null) {
                ObjC.dictionarySetObject(mutableInfo, elapsedValue, elapsedKey)
            }

            // Update playback rate using framework constant
            val rateKey = MPConstants.playbackRate
            val rateValue = ObjC.nsNumber(playbackRate)
            if (rateKey != null && rateValue != null) {
                ObjC.dictionarySetObject(mutableInfo, rateValue, rateKey)
            }

            // Set updated info
            ObjC.msg(infoCenter, "setNowPlayingInfo:", mutableInfo)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to update elapsed time: ${e.message}", e)
        }
    }

    /**
     * Clear Now Playing info
     */
    fun clearNowPlayingInfo() {
        if (!isSupported() || !initialized.get()) return

        try {
            val infoCenter = getNowPlayingInfoCenter() ?: return
            ObjC.msg(infoCenter, "setNowPlayingInfo:", null as Pointer?)
            ObjC.msg(infoCenter, "setPlaybackState:", MPNowPlayingPlaybackState.Stopped)
            Logger.d(TAG, "Cleared now playing info")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to clear now playing info: ${e.message}", e)
        }
    }

    /**
     * Release resources
     */
    fun release() {
        if (!isSupported()) return

        try {
            clearNowPlayingInfo()
            disableRemoteCommands()
            remoteCommandListener = null
            blockCallbacks.clear()
            blockMemories.clear()
            initialized.set(false)
            Logger.d(TAG, "Released MacOS media integration")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to release: ${e.message}", e)
        }
    }

    // ============ Artwork Methods ============

    /**
     * Load artwork from URL and update Now Playing info
     * This should be called from a coroutine
     */
    suspend fun loadAndSetArtwork(artworkUrl: String?) {
        if (artworkUrl.isNullOrEmpty()) {
            Logger.d(TAG, "No artwork URL provided")
            return
        }

        // Check if same artwork is already set
        if (artworkUrl == currentArtworkUrl && artworkCache.containsKey(artworkUrl)) {
            Logger.d(TAG, "Artwork already cached: $artworkUrl")
            return
        }

        try {
            // Download image data on IO dispatcher
            val imageData =
                withContext(Dispatchers.IO) {
                    downloadImageData(artworkUrl)
                }

            if (imageData == null) {
                Logger.e(TAG, "Failed to download artwork from: $artworkUrl")
                return
            }

            // Create NSImage and MPMediaItemArtwork on main thread
            val artwork = createArtwork(imageData)
            if (artwork != null) {
                artworkCache[artworkUrl] = artwork
                currentArtworkUrl = artworkUrl

                // Update the now playing info with artwork
                updateNowPlayingArtwork(artwork)
                Logger.d(TAG, "Successfully set artwork from: $artworkUrl")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error loading artwork: ${e.message}", e)
        }
    }

    /**
     * Download image data from URL
     */
    private fun downloadImageData(urlString: String): ByteArray? =
        try {
            val url = URL(urlString)
            url.openStream().use { it.readBytes() }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to download image: ${e.message}")
            null
        }

    /**
     * Create MPMediaItemArtwork from image data
     */
    private fun createArtwork(imageData: ByteArray): Pointer? {
        try {
            // Create NSData from bytes
            val nsData = createNSData(imageData) ?: return null

            // Create NSImage from NSData
            val nsImage = createNSImage(nsData) ?: return null

            // Create MPMediaItemArtwork
            // Use initWithBoundsSize:requestHandler: with a block that returns the image
            val artworkClass = ObjC.cls("MPMediaItemArtwork") ?: return null
            val allocated = ObjC.msg(artworkClass, "alloc") ?: return null

            // Get image size for bounds
            val sizeValue = ObjC.msg(nsImage, "size") // Returns NSSize struct

            // Create artwork with the image using a simpler approach
            // initWithImage: is deprecated but works for our purposes
            val artwork = ObjC.msg(allocated, "initWithImage:", nsImage)

            if (artwork == null) {
                Logger.w(TAG, "initWithImage: failed, trying alternative method")
                // Try alternative: store image and create artwork with bounds
                return createArtworkWithBounds(nsImage)
            }

            return artwork
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to create artwork: ${e.message}", e)
            return null
        }
    }

    /**
     * Create NSData from byte array
     */
    private fun createNSData(bytes: ByteArray): Pointer? {
        val nsDataClass = ObjC.cls("NSData") ?: return null

        // Copy bytes to native memory
        val nativeMemory = Memory(bytes.size.toLong())
        nativeMemory.write(0, bytes, 0, bytes.size)

        // Keep reference to prevent GC
        blockMemories.add(nativeMemory)

        // Create NSData with bytes:length:
        val allocated = ObjC.msg(nsDataClass, "alloc") ?: return null
        return ObjCRuntime.INSTANCE.objc_msgSend(
            allocated,
            ObjC.sel("initWithBytes:length:"),
            nativeMemory,
            bytes.size.toLong(),
        )
    }

    /**
     * Create NSImage from NSData
     */
    private fun createNSImage(nsData: Pointer): Pointer? {
        val nsImageClass = ObjC.cls("NSImage") ?: return null
        val allocated = ObjC.msg(nsImageClass, "alloc") ?: return null
        return ObjC.msg(allocated, "initWithData:", nsData)
    }

    /**
     * Alternative method to create artwork with bounds
     */
    private fun createArtworkWithBounds(nsImage: Pointer): Pointer? {
        // This is more complex as it requires creating a block
        // For now, return null and log
        Logger.w(TAG, "createArtworkWithBounds not fully implemented")
        return null
    }

    /**
     * Update the current now playing info with artwork
     */
    private fun updateNowPlayingArtwork(artwork: Pointer) {
        try {
            val infoCenter = getNowPlayingInfoCenter() ?: return

            // Get current now playing info
            val currentInfo = ObjC.msg(infoCenter, "nowPlayingInfo") ?: return

            // Create mutable copy
            val mutableInfo = ObjC.msg(currentInfo, "mutableCopy") ?: return

            // Set artwork
            val artworkKey = MPConstants.artwork
            if (artworkKey != null) {
                ObjC.dictionarySetObject(mutableInfo, artwork, artworkKey)
            }

            // Set updated info
            ObjC.msg(infoCenter, "setNowPlayingInfo:", mutableInfo)

            Logger.d(TAG, "Updated now playing artwork")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to update artwork: ${e.message}", e)
        }
    }

    // ============ Private helper methods ============

    private fun getNowPlayingInfoCenter(): Pointer? {
        val cls = ObjC.cls("MPNowPlayingInfoCenter") ?: return null
        return ObjC.msg(cls, "defaultCenter")
    }

    private fun getRemoteCommandCenter(): Pointer? {
        val cls = ObjC.cls("MPRemoteCommandCenter") ?: return null
        return ObjC.msg(cls, "sharedCommandCenter")
    }

    /**
     * Create a simple block that calls back to Kotlin
     * This is a simplified implementation - for production, consider using a library like JObjC
     */
    private fun createCommandHandler(handler: (Pointer?) -> Int): Pointer? {
        // Create a callback
        val callback =
            object : Callback {
                @Suppress("unused")
                fun invoke(
                    block: Pointer?,
                    event: Pointer?,
                ): Int {
                    Logger.d(TAG, "Block callback invoked! block=$block, event=$event")
                    return try {
                        handler(event)
                    } catch (e: Exception) {
                        Logger.e(TAG, "Command handler error: ${e.message}", e)
                        MPRemoteCommandHandlerStatus.CommandFailed
                    }
                }
            }

        // Keep reference to prevent GC
        blockCallbacks.add(callback)
        Logger.d(TAG, "Created callback: $callback")

        // Get the callback function pointer using CallbackReference
        val callbackPointer = CallbackReference.getFunctionPointer(callback)

        // Create block descriptor
        val descriptorSize = 40L // Size of BlockDescriptor
        val descriptorMemory = Memory(descriptorSize)
        descriptorMemory.clear()
        descriptorMemory.setLong(0, 0) // reserved
        descriptorMemory.setLong(8, 32) // size of block literal
        blockMemories.add(descriptorMemory)

        // Create block literal
        val blockSize = 32L
        val blockMemory = Memory(blockSize)
        blockMemory.clear()

        // Get NSConcreteStackBlock class (or use global block)
        val nsConcreteGlobalBlock =
            ObjC.cls("__NSGlobalBlock__")
                ?: ObjC.cls("NSBlock")
                ?: return null

        blockMemory.setPointer(0, nsConcreteGlobalBlock) // isa
        blockMemory.setInt(8, BlockLiteral.BLOCK_IS_GLOBAL) // flags
        blockMemory.setInt(12, 0) // reserved
        blockMemory.setPointer(16, callbackPointer) // invoke
        blockMemory.setPointer(24, descriptorMemory) // descriptor

        blockMemories.add(blockMemory)

        return blockMemory
    }

    private fun setupRemoteCommands(commandCenter: Pointer) {
        // Play command
        setupCommand(commandCenter, "playCommand") { _ ->
            remoteCommandListener?.onPlay()
            MPRemoteCommandHandlerStatus.Success
        }

        // Pause command
        setupCommand(commandCenter, "pauseCommand") { _ ->
            remoteCommandListener?.onPause()
            MPRemoteCommandHandlerStatus.Success
        }

        // Toggle play/pause command
        setupCommand(commandCenter, "togglePlayPauseCommand") { _ ->
            remoteCommandListener?.onTogglePlayPause()
            MPRemoteCommandHandlerStatus.Success
        }

        // Stop command
        setupCommand(commandCenter, "stopCommand") { _ ->
            remoteCommandListener?.onStop()
            MPRemoteCommandHandlerStatus.Success
        }

        // Next track command
        setupCommand(commandCenter, "nextTrackCommand") { _ ->
            remoteCommandListener?.onNextTrack()
            MPRemoteCommandHandlerStatus.Success
        }

        // Previous track command
        setupCommand(commandCenter, "previousTrackCommand") { _ ->
            remoteCommandListener?.onPreviousTrack()
            MPRemoteCommandHandlerStatus.Success
        }

        // Seek forward command
        setupCommand(commandCenter, "seekForwardCommand") { _ ->
            remoteCommandListener?.onSeekForward()
            MPRemoteCommandHandlerStatus.Success
        }

        // Seek backward command
        setupCommand(commandCenter, "seekBackwardCommand") { _ ->
            remoteCommandListener?.onSeekBackward()
            MPRemoteCommandHandlerStatus.Success
        }

        // Change playback position command (for seek bar scrubbing)
        setupChangePositionCommand(commandCenter)

        Logger.d(TAG, "Remote commands setup completed")
    }

    private fun setupCommand(
        commandCenter: Pointer,
        commandName: String,
        handler: (Pointer?) -> Int,
    ) {
        try {
            val command = ObjC.msg(commandCenter, commandName) ?: return

            // Enable the command using proper BOOL type (Byte)
            setCommandEnabled(command, 1)

            // Create handler block
            val handlerBlock = createCommandHandler(handler)
            if (handlerBlock != null) {
                ObjC.msg(command, "addTargetWithHandler:", handlerBlock)
            }
            Logger.d(TAG, "Setup command: $commandName")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to setup command $commandName: ${e.message}", e)
        }
    }

    private fun setupChangePositionCommand(commandCenter: Pointer) {
        try {
            val command = ObjC.msg(commandCenter, "changePlaybackPositionCommand") ?: return

            // Enable the command using proper BOOL type (Byte)
            setCommandEnabled(command, 1)

            // Create handler that extracts position from event
            val handlerBlock =
                createCommandHandler { event ->
                    Logger.d(TAG, "changePlaybackPositionCommand handler invoked! event=$event")
                    if (event != null) {
                        try {
                            // Get positionTime from MPChangePlaybackPositionCommandEvent
                            // Use ObjCRuntimeDouble for double return value (works on both ARM64 and x86_64)
                            val positionTime =
                                ObjCRuntimeDouble.INSTANCE.objc_msgSend(
                                    event,
                                    ObjC.sel("positionTime"),
                                )
                            Logger.d(TAG, "Seek position from system: $positionTime seconds")
                            remoteCommandListener?.onChangePlaybackPosition(positionTime)
                        } catch (e: Exception) {
                            Logger.e(TAG, "Failed to get position time: ${e.message}", e)
                        }
                    } else {
                        Logger.w(TAG, "changePlaybackPositionCommand: event is null!")
                    }
                    MPRemoteCommandHandlerStatus.Success
                }

            if (handlerBlock != null) {
                ObjC.msg(command, "addTargetWithHandler:", handlerBlock)
                Logger.d(TAG, "Setup command: changePlaybackPositionCommand with handler block: $handlerBlock")
            } else {
                Logger.e(TAG, "Failed to create handler block for changePlaybackPositionCommand")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to setup changePlaybackPositionCommand: ${e.message}", e)
        }
    }

    private fun disableRemoteCommands() {
        try {
            val commandCenter = getRemoteCommandCenter() ?: return

            val commands =
                listOf(
                    "playCommand",
                    "pauseCommand",
                    "togglePlayPauseCommand",
                    "stopCommand",
                    "nextTrackCommand",
                    "previousTrackCommand",
                    "seekForwardCommand",
                    "seekBackwardCommand",
                    "changePlaybackPositionCommand",
                )

            for (commandName in commands) {
                val command = ObjC.msg(commandCenter, commandName)
                if (command != null) {
                    setCommandEnabled(command, 0) // Disable using proper BOOL type
                    ObjC.msg(command, "removeTarget:", null as Pointer?)
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to disable remote commands: ${e.message}", e)
        }
    }
}

