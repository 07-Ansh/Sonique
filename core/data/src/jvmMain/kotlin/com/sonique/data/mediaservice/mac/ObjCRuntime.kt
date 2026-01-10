package com.sonique.data.mediaservice.mac

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

 
interface ObjCRuntime : Library {
    companion object {
        val INSTANCE: ObjCRuntime by lazy {
            Native.load("objc", ObjCRuntime::class.java)
        }
    }

     
    fun objc_getClass(name: String): Pointer?

     
    fun sel_registerName(name: String): Pointer?

     
    fun objc_msgSend(
        receiver: Pointer?,
        selector: Pointer?,
    ): Pointer?

     
    fun objc_msgSend(
        receiver: Pointer?,
        selector: Pointer?,
        arg1: Pointer?,
    ): Pointer?

     
    fun objc_msgSend(
        receiver: Pointer?,
        selector: Pointer?,
        arg1: Pointer?,
        arg2: Pointer?,
    ): Pointer?

     
    fun objc_msgSend(
        receiver: Pointer?,
        selector: Pointer?,
        arg1: Double,
    ): Pointer?

     
    fun objc_msgSend(
        receiver: Pointer?,
        selector: Pointer?,
        arg1: Long,
    ): Pointer?

     
    fun objc_msgSend(
        receiver: Pointer?,
        selector: Pointer?,
        arg1: Int,
    ): Pointer?

     
    fun objc_msgSend(
        receiver: Pointer?,
        selector: Pointer?,
        arg1: Boolean,
    ): Pointer?

     
    fun objc_msgSend(
        receiver: Pointer?,
        selector: Pointer?,
        arg1: Byte,
    ): Pointer?

     
    fun objc_msgSend(
        receiver: Pointer?,
        selector: Pointer?,
        cString: String,
    ): Pointer?

     
    fun objc_msgSend(
        receiver: Pointer?,
        selector: Pointer?,
        bytes: Pointer?,
        length: Long,
    ): Pointer?

     
    fun objc_msgSend_fpret(
        receiver: Pointer?,
        selector: Pointer?,
    ): Double

     
    fun class_getName(cls: Pointer?): String?
}

 
interface ObjCRuntimeDouble : Library {
    companion object {
        val INSTANCE: ObjCRuntimeDouble by lazy {
            Native.load("objc", ObjCRuntimeDouble::class.java)
        }
    }

     
    fun objc_msgSend(
        receiver: Pointer?,
        selector: Pointer?,
    ): Double
}

 
interface Foundation : Library {
    companion object {
        val INSTANCE: Foundation by lazy {
            Native.load("Foundation", Foundation::class.java)
        }
    }

    fun NSClassFromString(className: String): Pointer?
}

 
interface CoreFoundation : Library {
    companion object {
        val INSTANCE: CoreFoundation by lazy {
            Native.load("CoreFoundation", CoreFoundation::class.java)
        }

         
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

     
    fun CFStringCreateWithCString(
        alloc: Pointer?,
        cStr: String,
        encoding: Int,
    ): Pointer?

     
    fun CFNumberCreate(
        allocator: Pointer?,
        theType: Int,
        valuePtr: Pointer?,
    ): Pointer?
}

 
interface ObjCBlock : Callback {
    fun invoke(
        block: Pointer?,
        event: Pointer?,
    ): Int
}

 
object ObjC {
    private val runtime = ObjCRuntime.INSTANCE

     
    private val selectorCache = mutableMapOf<String, Pointer?>()

     
    private val classCache = mutableMapOf<String, Pointer?>()

     
    fun sel(name: String): Pointer? =
        selectorCache.getOrPut(name) {
            runtime.sel_registerName(name)
        }

     
    fun cls(name: String): Pointer? =
        classCache.getOrPut(name) {
            runtime.objc_getClass(name)
        }

     
    fun msg(
        receiver: Pointer?,
        selector: String,
    ): Pointer? = runtime.objc_msgSend(receiver, sel(selector))

     
    fun msg(
        receiver: Pointer?,
        selector: String,
        arg1: Pointer?,
    ): Pointer? = runtime.objc_msgSend(receiver, sel(selector), arg1)

     
    fun msg(
        receiver: Pointer?,
        selector: String,
        arg1: Pointer?,
        arg2: Pointer?,
    ): Pointer? = runtime.objc_msgSend(receiver, sel(selector), arg1, arg2)

     
    fun msg(
        receiver: Pointer?,
        selector: String,
        arg1: Double,
    ): Pointer? = runtime.objc_msgSend(receiver, sel(selector), arg1)

     
    fun msg(
        receiver: Pointer?,
        selector: String,
        arg1: Long,
    ): Pointer? = runtime.objc_msgSend(receiver, sel(selector), arg1)

     
    fun msg(
        receiver: Pointer?,
        selector: String,
        arg1: Int,
    ): Pointer? = runtime.objc_msgSend(receiver, sel(selector), arg1)

     
    fun msg(
        receiver: Pointer?,
        selector: String,
        arg1: Boolean,
    ): Pointer? = runtime.objc_msgSend(receiver, sel(selector), arg1)

     
    fun msgWithCString(
        receiver: Pointer?,
        selector: String,
        cString: String,
    ): Pointer? = runtime.objc_msgSend(receiver, sel(selector), cString)

     
    fun nsString(str: String): Pointer? {
         
        return CoreFoundation.INSTANCE.CFStringCreateWithCString(
            null,
            str,
            CoreFoundation.kCFStringEncodingUTF8,
        )
    }

     
    fun nsNumber(value: Double): Pointer? {
        val nsNumberClass = cls("NSNumber") ?: return null
        return runtime.objc_msgSend(nsNumberClass, sel("numberWithDouble:"), value)
    }

     
    fun nsNumber(value: Long): Pointer? {
        val nsNumberClass = cls("NSNumber") ?: return null
        return runtime.objc_msgSend(nsNumberClass, sel("numberWithLongLong:"), value)
    }

     
    fun nsNumber(value: Int): Pointer? {
        val nsNumberClass = cls("NSNumber") ?: return null
        return runtime.objc_msgSend(nsNumberClass, sel("numberWithInt:"), value)
    }

     
    fun nsUrl(urlString: String): Pointer? {
        val nsUrlClass = cls("NSURL") ?: return null
        val nsStr = nsString(urlString) ?: return null
        return msg(nsUrlClass, "URLWithString:", nsStr)
    }

     
    fun nsMutableDictionary(): Pointer? {
        val dictClass = cls("NSMutableDictionary") ?: return null
        val allocated = msg(dictClass, "alloc") ?: return null
        return msg(allocated, "init")
    }

     
    fun dictionarySetObject(
        dict: Pointer?,
        obj: Pointer?,
        key: Pointer?,
    ) {
        msg(dict, "setObject:forKey:", obj, key)
    }
}

