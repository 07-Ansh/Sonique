package com.sonique.data.mediaservice.mac

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

/**
 * JNA interface for Objective-C Runtime on macOS
 * Used to call Cocoa/MediaPlayer framework APIs
 */
interface ObjCRuntime : Library {
    companion object {
        val INSTANCE: ObjCRuntime by lazy {
            Native.load("objc", ObjCRuntime::class.java)
        }
    }

    /**
     * Get a class by name
     * @param name The name of the class (e.g., "NSString", "MPNowPlayingInfoCenter")
     */
    fun objc_getClass(name: String): Pointer?

    /**
     * Register/get a selector by name
     * @param name The selector name (e.g., "alloc", "init", "defaultCenter")
     */
    fun sel_registerName(name: String): Pointer?

    /**
     * Send a message to an object (no arguments, returns Pointer)
     */
    fun objc_msgSend(
        receiver: Pointer?,
        selector: Pointer?,
    ): Pointer?

    /**
     * Send a message with one Pointer argument
     */
    fun objc_msgSend(
        receiver: Pointer?,
        selector: Pointer?,
        arg1: Pointer?,
    ): Pointer?

    /**
     * Send a message with two Pointer arguments
     */
    fun objc_msgSend(
        receiver: Pointer?,
        selector: Pointer?,
        arg1: Pointer?,
        arg2: Pointer?,
    ): Pointer?

    /**
     * Send a message with one double argument
     */
    fun objc_msgSend(
        receiver: Pointer?,
        selector: Pointer?,
        arg1: Double,
    ): Pointer?

    /**
     * Send a message with one long argument
     */
    fun objc_msgSend(
        receiver: Pointer?,
        selector: Pointer?,
        arg1: Long,
    ): Pointer?

    /**
     * Send a message with one int argument
     */
    fun objc_msgSend(
        receiver: Pointer?,
        selector: Pointer?,
        arg1: Int,
    ): Pointer?

    /**
     * Send a message with one boolean argument
     */
    fun objc_msgSend(
        receiver: Pointer?,
        selector: Pointer?,
        arg1: Boolean,
    ): Pointer?

    /**
     * Send a message with one byte argument (for Objective-C BOOL which is signed char)
     */
    fun objc_msgSend(
        receiver: Pointer?,
        selector: Pointer?,
        arg1: Byte,
    ): Pointer?

    /**
     * Send a message with C string argument (for initWithUTF8String:)
     * JNA will automatically convert String to const char*
     */
    fun objc_msgSend(
        receiver: Pointer?,
        selector: Pointer?,
        cString: String,
    ): Pointer?

    /**
     * Send a message with Pointer and Long arguments (for initWithBytes:length:)
     */
    fun objc_msgSend(
        receiver: Pointer?,
        selector: Pointer?,
        bytes: Pointer?,
        length: Long,
    ): Pointer?

    /**
     * Send a message that returns a double
     * Note: objc_msgSend_fpret only exists on x86_64, not on ARM64
     * On ARM64, use ObjCRuntimeDouble.INSTANCE.objc_msgSend instead
     */
    fun objc_msgSend_fpret(
        receiver: Pointer?,
        selector: Pointer?,
    ): Double

    /**
     * Allocate a block for callback
     */
    fun class_getName(cls: Pointer?): String?
}

/**
 * Separate interface for objc_msgSend that returns Double
 * This is needed because JNA needs different function signatures for different return types
 * Works on both ARM64 and x86_64
 */
interface ObjCRuntimeDouble : Library {
    companion object {
        val INSTANCE: ObjCRuntimeDouble by lazy {
            Native.load("objc", ObjCRuntimeDouble::class.java)
        }
    }

    /**
     * Send a message that returns a double value
     * On ARM64, this uses the regular objc_msgSend
     * JNA will handle the return type correctly
     */
    fun objc_msgSend(
        receiver: Pointer?,
        selector: Pointer?,
    ): Double
}

/**
 * JNA interface for Foundation framework
 */
interface Foundation : Library {
    companion object {
        val INSTANCE: Foundation by lazy {
            Native.load("Foundation", Foundation::class.java)
        }
    }

    fun NSClassFromString(className: String): Pointer?
}

/**
 * JNA interface for CoreFoundation framework
 */
interface CoreFoundation : Library {
    companion object {
        val INSTANCE: CoreFoundation by lazy {
            Native.load("CoreFoundation", CoreFoundation::class.java)
        }

        // CFStringEncoding values
        const val kCFStringEncodingUTF8 = 0x08000100
    }

    fun CFRunLoopGetMain(): Pointer?

    fun CFRunLoopGetCurrent(): Pointer?

    fun CFRunLoopRun()

    fun CFRunLoopRunInMode(
        mode: Pointer?,
        seconds: Double,
        returnAfterSourceHandled: Boolean,
    ): Int

    /**
     * Create a CFString from a C string
     * CFString is toll-free bridged with NSString
     * @param alloc Pass null for default allocator
     * @param cStr The C string (JNA auto-converts String to const char*)
     * @param encoding Use kCFStringEncodingUTF8
     */
    fun CFStringCreateWithCString(
        alloc: Pointer?,
        cStr: String,
        encoding: Int,
    ): Pointer?

    /**
     * Create a CFNumber from a double
     * CFNumber is toll-free bridged with NSNumber
     */
    fun CFNumberCreate(
        allocator: Pointer?,
        theType: Int,
        valuePtr: Pointer?,
    ): Pointer?
}

/**
 * Block callback interface for Objective-C blocks
 */
interface ObjCBlock : Callback {
    fun invoke(
        block: Pointer?,
        event: Pointer?,
    ): Int
}

/**
 * Helper object for Objective-C operations
 */
object ObjC {
    private val runtime = ObjCRuntime.INSTANCE

    // Cache for frequently used selectors
    private val selectorCache = mutableMapOf<String, Pointer?>()

    // Cache for frequently used classes
    private val classCache = mutableMapOf<String, Pointer?>()

    /**
     * Get a cached selector
     */
    fun sel(name: String): Pointer? =
        selectorCache.getOrPut(name) {
            runtime.sel_registerName(name)
        }

    /**
     * Get a cached class
     */
    fun cls(name: String): Pointer? =
        classCache.getOrPut(name) {
            runtime.objc_getClass(name)
        }

    /**
     * Send a message with no arguments
     */
    fun msg(
        receiver: Pointer?,
        selector: String,
    ): Pointer? = runtime.objc_msgSend(receiver, sel(selector))

    /**
     * Send a message with one Pointer argument
     */
    fun msg(
        receiver: Pointer?,
        selector: String,
        arg1: Pointer?,
    ): Pointer? = runtime.objc_msgSend(receiver, sel(selector), arg1)

    /**
     * Send a message with two Pointer arguments
     */
    fun msg(
        receiver: Pointer?,
        selector: String,
        arg1: Pointer?,
        arg2: Pointer?,
    ): Pointer? = runtime.objc_msgSend(receiver, sel(selector), arg1, arg2)

    /**
     * Send a message with one double argument
     */
    fun msg(
        receiver: Pointer?,
        selector: String,
        arg1: Double,
    ): Pointer? = runtime.objc_msgSend(receiver, sel(selector), arg1)

    /**
     * Send a message with one long argument
     */
    fun msg(
        receiver: Pointer?,
        selector: String,
        arg1: Long,
    ): Pointer? = runtime.objc_msgSend(receiver, sel(selector), arg1)

    /**
     * Send a message with one int argument
     */
    fun msg(
        receiver: Pointer?,
        selector: String,
        arg1: Int,
    ): Pointer? = runtime.objc_msgSend(receiver, sel(selector), arg1)

    /**
     * Send a message with one boolean argument
     */
    fun msg(
        receiver: Pointer?,
        selector: String,
        arg1: Boolean,
    ): Pointer? = runtime.objc_msgSend(receiver, sel(selector), arg1)

    /**
     * Send a message with C string argument
     */
    fun msgWithCString(
        receiver: Pointer?,
        selector: String,
        cString: String,
    ): Pointer? = runtime.objc_msgSend(receiver, sel(selector), cString)

    /**
     * Create an NSString from a Kotlin String
     * Uses CFStringCreateWithCString which is more reliable with JNA
     * CFString is toll-free bridged with NSString
     */
    fun nsString(str: String): Pointer? {
        // Use CoreFoundation - CFString is toll-free bridged with NSString
        return CoreFoundation.INSTANCE.CFStringCreateWithCString(
            null,
            str,
            CoreFoundation.kCFStringEncodingUTF8,
        )
    }

    /**
     * Create an NSNumber from a Double
     */
    fun nsNumber(value: Double): Pointer? {
        val nsNumberClass = cls("NSNumber") ?: return null
        return runtime.objc_msgSend(nsNumberClass, sel("numberWithDouble:"), value)
    }

    /**
     * Create an NSNumber from a Long
     */
    fun nsNumber(value: Long): Pointer? {
        val nsNumberClass = cls("NSNumber") ?: return null
        return runtime.objc_msgSend(nsNumberClass, sel("numberWithLongLong:"), value)
    }

    /**
     * Create an NSNumber from an Int
     */
    fun nsNumber(value: Int): Pointer? {
        val nsNumberClass = cls("NSNumber") ?: return null
        return runtime.objc_msgSend(nsNumberClass, sel("numberWithInt:"), value)
    }

    /**
     * Create an NSURL from a String
     */
    fun nsUrl(urlString: String): Pointer? {
        val nsUrlClass = cls("NSURL") ?: return null
        val nsStr = nsString(urlString) ?: return null
        return msg(nsUrlClass, "URLWithString:", nsStr)
    }

    /**
     * Create an NSMutableDictionary
     */
    fun nsMutableDictionary(): Pointer? {
        val dictClass = cls("NSMutableDictionary") ?: return null
        val allocated = msg(dictClass, "alloc") ?: return null
        return msg(allocated, "init")
    }

    /**
     * Set object in dictionary for key
     */
    fun dictionarySetObject(
        dict: Pointer?,
        obj: Pointer?,
        key: Pointer?,
    ) {
        msg(dict, "setObject:forKey:", obj, key)
    }
}

