package com.liskovsoft.sharedutils.cronet

import android.content.Context
import com.liskovsoft.sharedutils.mylogger.Log
import org.chromium.net.CronetEngine
import org.chromium.net.impl.NativeCronetProvider

object CronetManager {
    private val TAG = CronetManager::class.java.simpleName
    private var engine: CronetEngine? = null

    @JvmStatic
    fun getEngine(context: Context): CronetEngine? {
        if (engine == null) {
            //val wrapper = CronetEngineWrapper(context)
            //engine = wrapper.cronetEngine

            //engine = CronetEngine.Builder(context)
            //    .enableQuic(true)
            //    .enableHttp2(true)
            //    .enableBrotli(true)
            //    .build()

            try {
                engine = NativeCronetProvider(context)
                    .createBuilder()
                    .enableQuic(true)
                    .enableHttp2(true)
                    .enableBrotli(true)
                    //.addQuicHint("youtube.com", 80, 80)
                    .build()
            } catch (e: UnsatisfiedLinkError) {
                // Fatal Exception: java.lang.UnsatisfiedLinkError
                // Cannot load library: soinfo_relocate(linker.cpp:982): cannot locate symbol "getauxval" referenced by "libcronet.101.0.4951.41.so".
                e.printStackTrace()
                Log.e(TAG, e.message)
            }
        }

        return engine
    }
}

