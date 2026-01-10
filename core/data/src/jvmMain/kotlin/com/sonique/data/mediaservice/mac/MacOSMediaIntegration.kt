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

 
object MPNowPlayingPlaybackState {
    const val Unknown = 0
    const val Playing = 1
    const val Paused = 2
    const val Stopped = 3
    const val Interrupted = 4
}

 
object MPRemoteCommandHandlerStatus {
    const val Success = 0
    const val NoSuchContent = 1
    const val NoActionableNowPlayingItem = 2
    const val DeviceNotFound = 3
    const val CommandFailed = 4
}

 
object MPMediaItemProperty {
     
    const val Title = "title"
    const val Artist = "artist"
    const val Album = "albumTitle"
    const val PlaybackDuration = "playbackDuration"
    const val Artwork = "artwork"
}

object MPNowPlayingInfoProperty {
     
    const val ElapsedPlaybackTime = "MPNowPlayingInfoPropertyElapsedPlaybackTime"
    const val PlaybackRate = "MPNowPlayingInfoPropertyPlaybackRate"
    const val DefaultPlaybackRate = "MPNowPlayingInfoPropertyDefaultPlaybackRate"
    const val PlaybackQueueIndex = "MPNowPlayingInfoPropertyPlaybackQueueIndex"
    const val PlaybackQueueCount = "MPNowPlayingInfoPropertyPlaybackQueueCount"
    const val MediaType = "MPNowPlayingInfoPropertyMediaType"
}

 
object MPConstants {
    private var loaded = false
    private val constants = mutableMapOf<String, Pointer?>()

    private fun ensureLoaded() {
        if (loaded) return
        try {
             
            val lib =
                com.sun.jna.NativeLibrary
                    .getInstance("MediaPlayer")

             
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
         
        const val BLOCK_HAS_COPY_DISPOSE = (1 shl 25)
        const val BLOCK_HAS_CTOR = (1 shl 26)
        const val BLOCK_IS_GLOBAL = (1 shl 28)
        const val BLOCK_HAS_STRET = (1 shl 29)
        const val BLOCK_HAS_SIGNATURE = (1 shl 30)
    }
}

 
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

 
class MacOSMediaIntegration private constructor() {
    private val initialized = AtomicBoolean(false)
    private var remoteCommandListener: MacOSRemoteCommandListener? = null

     
    private val blockCallbacks = mutableListOf<Callback>()
    private val blockMemories = mutableListOf<Memory>()

     
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

         
        fun isSupported(): Boolean = Platform.isMac()

         
        private fun loadMediaPlayerFramework(): Boolean {
            if (frameworkLoaded) return true

            return try {
                 
                 
                val frameworkPath = "/System/Library/Frameworks/MediaPlayer.framework/MediaPlayer"
                System.load(frameworkPath)
                frameworkLoaded = true
                Logger.d(TAG, "MediaPlayer framework loaded successfully")
                true
            } catch (e: UnsatisfiedLinkError) {
                 
                 
                Logger.w(TAG, "Could not load MediaPlayer framework directly: ${e.message}")
                 
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
             
            if (!loadMediaPlayerFramework()) {
                Logger.e(TAG, "Failed to load MediaPlayer framework")
                return false
            }

             
            val commandCenter = getRemoteCommandCenter()
            if (commandCenter == null) {
                Logger.e(TAG, "Failed to get MPRemoteCommandCenter")
                return false
            }

             
            setupRemoteCommands(commandCenter)

            initialized.set(true)
            Logger.d(TAG, "MacOS media integration initialized successfully")
            return true
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to initialize MacOS media integration: ${e.message}", e)
            return false
        }
    }

     
    fun setRemoteCommandListener(listener: MacOSRemoteCommandListener?) {
        this.remoteCommandListener = listener
    }

     
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

             
             
            val titleKey = MPConstants.title
            val titleValue = ObjC.nsString(info.title)
            if (titleKey != null && titleValue != null) {
                ObjC.dictionarySetObject(dict, titleValue, titleKey)
                Logger.d(TAG, "Set title: ${info.title} (key: $titleKey)")
            } else {
                Logger.e(TAG, "Failed to set title - key: $titleKey, value: $titleValue")
            }

             
            val artistKey = MPConstants.artist
            val artistValue = ObjC.nsString(info.artist)
            if (artistKey != null && artistValue != null) {
                ObjC.dictionarySetObject(dict, artistValue, artistKey)
                Logger.d(TAG, "Set artist: ${info.artist}")
            } else {
                Logger.e(TAG, "Failed to set artist - key: $artistKey")
            }

             
            if (info.album.isNotEmpty()) {
                val albumKey = MPConstants.album
                val albumValue = ObjC.nsString(info.album)
                if (albumKey != null && albumValue != null) {
                    ObjC.dictionarySetObject(dict, albumValue, albumKey)
                }
            }

             
            val durationKey = MPConstants.playbackDuration
            val durationValue = ObjC.nsNumber(info.durationSeconds)
            if (durationKey != null && durationValue != null) {
                ObjC.dictionarySetObject(dict, durationValue, durationKey)
                Logger.d(TAG, "Set duration: ${info.durationSeconds}")
            } else {
                Logger.e(TAG, "Failed to set duration - key: $durationKey, value: $durationValue")
            }

             
            val elapsedKey = MPConstants.elapsedPlaybackTime
            val elapsedValue = ObjC.nsNumber(info.elapsedTimeSeconds)
            if (elapsedKey != null && elapsedValue != null) {
                ObjC.dictionarySetObject(dict, elapsedValue, elapsedKey)
            } else {
                Logger.w(TAG, "Failed to set elapsed time - key: $elapsedKey")
            }

             
            val rateKey = MPConstants.playbackRate
            val rateValue = ObjC.nsNumber(info.playbackRate)
            if (rateKey != null && rateValue != null) {
                ObjC.dictionarySetObject(dict, rateValue, rateKey)
            }

             
            val defaultRateKey = MPConstants.defaultPlaybackRate
            val defaultRateValue = ObjC.nsNumber(1.0)
            if (defaultRateKey != null && defaultRateValue != null) {
                ObjC.dictionarySetObject(dict, defaultRateValue, defaultRateKey)
            }

             
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

             
            val mediaTypeKey = MPConstants.mediaType
            val mediaTypeValue = ObjC.nsNumber(1)
            if (mediaTypeKey != null && mediaTypeValue != null) {
                ObjC.dictionarySetObject(dict, mediaTypeValue, mediaTypeKey)
            }

             
            val countSel = ObjC.sel("count")
            if (countSel != null) {
                val count = ObjCRuntime.INSTANCE.objc_msgSend(dict, countSel)
                Logger.d(TAG, "Dictionary has entries (pointer: $count)")
            }

             
            ObjC.msg(infoCenter, "setNowPlayingInfo:", dict)

            Logger.d(TAG, "Updated now playing info: ${info.title} - ${info.artist}")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to update now playing info: ${e.message}", e)
        }
    }

     
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

     
    fun updateCommandsEnabled(
        hasNext: Boolean,
        hasPrevious: Boolean,
        canSeek: Boolean = true,
    ) {
        if (!isSupported() || !initialized.get()) return

        try {
            val commandCenter = getRemoteCommandCenter() ?: return

             
             
            val nextEnabled: Byte = if (hasNext) 1 else 0
            val prevEnabled: Byte = if (hasPrevious) 1 else 0
            val seekEnabled: Byte = if (canSeek) 1 else 0

             
            val nextCommand = ObjC.msg(commandCenter, "nextTrackCommand")
            if (nextCommand != null) {
                setCommandEnabled(nextCommand, nextEnabled)
                Logger.d(TAG, "Next command enabled: $hasNext")
            }

             
            val prevCommand = ObjC.msg(commandCenter, "previousTrackCommand")
            if (prevCommand != null) {
                setCommandEnabled(prevCommand, prevEnabled)
                Logger.d(TAG, "Previous command enabled: $hasPrevious")
            }

             
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

     
    private fun setCommandEnabled(
        command: Pointer,
        enabled: Byte,
    ) {
         
        val enabledInt = enabled.toInt()
        ObjCRuntime.INSTANCE.objc_msgSend(command, ObjC.sel("setEnabled:"), enabledInt)
    }

     
    fun updateElapsedTime(
        elapsedSeconds: Double,
        playbackRate: Double = 1.0,
    ) {
        if (!isSupported() || !initialized.get()) return

        try {
            val infoCenter = getNowPlayingInfoCenter() ?: return

             
            val currentInfo = ObjC.msg(infoCenter, "nowPlayingInfo")
            if (currentInfo == null) {
                Logger.w(TAG, "No current now playing info to update")
                return
            }

             
            val mutableInfo = ObjC.msg(currentInfo, "mutableCopy") ?: return

             
            val elapsedKey = MPConstants.elapsedPlaybackTime
            val elapsedValue = ObjC.nsNumber(elapsedSeconds)
            if (elapsedKey != null && elapsedValue != null) {
                ObjC.dictionarySetObject(mutableInfo, elapsedValue, elapsedKey)
            }

             
            val rateKey = MPConstants.playbackRate
            val rateValue = ObjC.nsNumber(playbackRate)
            if (rateKey != null && rateValue != null) {
                ObjC.dictionarySetObject(mutableInfo, rateValue, rateKey)
            }

             
            ObjC.msg(infoCenter, "setNowPlayingInfo:", mutableInfo)
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to update elapsed time: ${e.message}", e)
        }
    }

     
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

     

     
    suspend fun loadAndSetArtwork(artworkUrl: String?) {
        if (artworkUrl.isNullOrEmpty()) {
            Logger.d(TAG, "No artwork URL provided")
            return
        }

         
        if (artworkUrl == currentArtworkUrl && artworkCache.containsKey(artworkUrl)) {
            Logger.d(TAG, "Artwork already cached: $artworkUrl")
            return
        }

        try {
             
            val imageData =
                withContext(Dispatchers.IO) {
                    downloadImageData(artworkUrl)
                }

            if (imageData == null) {
                Logger.e(TAG, "Failed to download artwork from: $artworkUrl")
                return
            }

             
            val artwork = createArtwork(imageData)
            if (artwork != null) {
                artworkCache[artworkUrl] = artwork
                currentArtworkUrl = artworkUrl

                 
                updateNowPlayingArtwork(artwork)
                Logger.d(TAG, "Successfully set artwork from: $artworkUrl")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error loading artwork: ${e.message}", e)
        }
    }

     
    private fun downloadImageData(urlString: String): ByteArray? =
        try {
            val url = URL(urlString)
            url.openStream().use { it.readBytes() }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to download image: ${e.message}")
            null
        }

     
    private fun createArtwork(imageData: ByteArray): Pointer? {
        try {
             
            val nsData = createNSData(imageData) ?: return null

             
            val nsImage = createNSImage(nsData) ?: return null

             
             
            val artworkClass = ObjC.cls("MPMediaItemArtwork") ?: return null
            val allocated = ObjC.msg(artworkClass, "alloc") ?: return null

             
            val sizeValue = ObjC.msg(nsImage, "size")  

             
             
            val artwork = ObjC.msg(allocated, "initWithImage:", nsImage)

            if (artwork == null) {
                Logger.w(TAG, "initWithImage: failed, trying alternative method")
                 
                return createArtworkWithBounds(nsImage)
            }

            return artwork
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to create artwork: ${e.message}", e)
            return null
        }
    }

     
    private fun createNSData(bytes: ByteArray): Pointer? {
        val nsDataClass = ObjC.cls("NSData") ?: return null

         
        val nativeMemory = Memory(bytes.size.toLong())
        nativeMemory.write(0, bytes, 0, bytes.size)

         
        blockMemories.add(nativeMemory)

         
        val allocated = ObjC.msg(nsDataClass, "alloc") ?: return null
        return ObjCRuntime.INSTANCE.objc_msgSend(
            allocated,
            ObjC.sel("initWithBytes:length:"),
            nativeMemory,
            bytes.size.toLong(),
        )
    }

     
    private fun createNSImage(nsData: Pointer): Pointer? {
        val nsImageClass = ObjC.cls("NSImage") ?: return null
        val allocated = ObjC.msg(nsImageClass, "alloc") ?: return null
        return ObjC.msg(allocated, "initWithData:", nsData)
    }

     
    private fun createArtworkWithBounds(nsImage: Pointer): Pointer? {
         
         
        Logger.w(TAG, "createArtworkWithBounds not fully implemented")
        return null
    }

     
    private fun updateNowPlayingArtwork(artwork: Pointer) {
        try {
            val infoCenter = getNowPlayingInfoCenter() ?: return

             
            val currentInfo = ObjC.msg(infoCenter, "nowPlayingInfo") ?: return

             
            val mutableInfo = ObjC.msg(currentInfo, "mutableCopy") ?: return

             
            val artworkKey = MPConstants.artwork
            if (artworkKey != null) {
                ObjC.dictionarySetObject(mutableInfo, artwork, artworkKey)
            }

             
            ObjC.msg(infoCenter, "setNowPlayingInfo:", mutableInfo)

            Logger.d(TAG, "Updated now playing artwork")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to update artwork: ${e.message}", e)
        }
    }

     

    private fun getNowPlayingInfoCenter(): Pointer? {
        val cls = ObjC.cls("MPNowPlayingInfoCenter") ?: return null
        return ObjC.msg(cls, "defaultCenter")
    }

    private fun getRemoteCommandCenter(): Pointer? {
        val cls = ObjC.cls("MPRemoteCommandCenter") ?: return null
        return ObjC.msg(cls, "sharedCommandCenter")
    }

     
    private fun createCommandHandler(handler: (Pointer?) -> Int): Pointer? {
         
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

         
        blockCallbacks.add(callback)
        Logger.d(TAG, "Created callback: $callback")

         
        val callbackPointer = CallbackReference.getFunctionPointer(callback)

         
        val descriptorSize = 40L  
        val descriptorMemory = Memory(descriptorSize)
        descriptorMemory.clear()
        descriptorMemory.setLong(0, 0)  
        descriptorMemory.setLong(8, 32)  
        blockMemories.add(descriptorMemory)

         
        val blockSize = 32L
        val blockMemory = Memory(blockSize)
        blockMemory.clear()

         
        val nsConcreteGlobalBlock =
            ObjC.cls("__NSGlobalBlock__")
                ?: ObjC.cls("NSBlock")
                ?: return null

        blockMemory.setPointer(0, nsConcreteGlobalBlock)  
        blockMemory.setInt(8, BlockLiteral.BLOCK_IS_GLOBAL)  
        blockMemory.setInt(12, 0)  
        blockMemory.setPointer(16, callbackPointer)  
        blockMemory.setPointer(24, descriptorMemory)  

        blockMemories.add(blockMemory)

        return blockMemory
    }

    private fun setupRemoteCommands(commandCenter: Pointer) {
         
        setupCommand(commandCenter, "playCommand") { _ ->
            remoteCommandListener?.onPlay()
            MPRemoteCommandHandlerStatus.Success
        }

         
        setupCommand(commandCenter, "pauseCommand") { _ ->
            remoteCommandListener?.onPause()
            MPRemoteCommandHandlerStatus.Success
        }

         
        setupCommand(commandCenter, "togglePlayPauseCommand") { _ ->
            remoteCommandListener?.onTogglePlayPause()
            MPRemoteCommandHandlerStatus.Success
        }

         
        setupCommand(commandCenter, "stopCommand") { _ ->
            remoteCommandListener?.onStop()
            MPRemoteCommandHandlerStatus.Success
        }

         
        setupCommand(commandCenter, "nextTrackCommand") { _ ->
            remoteCommandListener?.onNextTrack()
            MPRemoteCommandHandlerStatus.Success
        }

         
        setupCommand(commandCenter, "previousTrackCommand") { _ ->
            remoteCommandListener?.onPreviousTrack()
            MPRemoteCommandHandlerStatus.Success
        }

         
        setupCommand(commandCenter, "seekForwardCommand") { _ ->
            remoteCommandListener?.onSeekForward()
            MPRemoteCommandHandlerStatus.Success
        }

         
        setupCommand(commandCenter, "seekBackwardCommand") { _ ->
            remoteCommandListener?.onSeekBackward()
            MPRemoteCommandHandlerStatus.Success
        }

         
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

             
            setCommandEnabled(command, 1)

             
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

             
            setCommandEnabled(command, 1)

             
            val handlerBlock =
                createCommandHandler { event ->
                    Logger.d(TAG, "changePlaybackPositionCommand handler invoked! event=$event")
                    if (event != null) {
                        try {
                             
                             
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
                    setCommandEnabled(command, 0)  
                    ObjC.msg(command, "removeTarget:", null as Pointer?)
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to disable remote commands: ${e.message}", e)
        }
    }
}

