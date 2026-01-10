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
             
             

             
             
             
             
             

            try {
                engine = NativeCronetProvider(context)
                    .createBuilder()
                    .enableQuic(true)
                    .enableHttp2(true)
                    .enableBrotli(true)
                     
                    .build()
            } catch (e: UnsatisfiedLinkError) {
                 
                 
                e.printStackTrace()
                Log.e(TAG, e.message)
            }
        }

        return engine
    }
}

