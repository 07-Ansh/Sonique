package com.liskovsoft.youtubeapi.app.potokennp2

import android.content.Context
import java.io.Closeable

 
internal interface PoTokenGenerator : Closeable {
     
    fun generatePoToken(identifier: String): String

     
    fun isExpired(): Boolean

    interface Factory {
         
        fun newPoTokenGenerator(context: Context): PoTokenGenerator
    }
}


