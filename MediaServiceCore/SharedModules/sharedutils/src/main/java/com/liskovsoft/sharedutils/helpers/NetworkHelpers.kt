package com.liskovsoft.sharedutils.helpers

import android.os.Build
import com.liskovsoft.sharedutils.mylogger.Log
import com.liskovsoft.sharedutils.okhttp.DohProviders
import info.guardianproject.netcipher.NetCipher
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection

object NetworkHelpers {
    @JvmStatic
    fun getHttpsURLConnection(url: URL): HttpURLConnection {
         
         
        val conn = getBestHttpsURLConnection(url)

         
         
         

        return conn
    }

     
    @JvmStatic
    fun getDohURLConnection(url: URL): HttpURLConnection {
        if (Build.VERSION.SDK_INT <= 19) {
            return getBestHttpsURLConnection(url)
        }

        return DohProviders.cachedGoogle?.let {
            val ipAddress = it.lookup(url.host)

            val fullUrl = "${url.protocol}://${ipAddress[0].hostName}${url.path}?${url.query}"
            Log.d("NetworkHelpers", fullUrl)

            val conn = getBestHttpsURLConnection(URL(fullUrl))
             
            conn.hostnameVerifier = HostnameVerifier { _, _ -> true }
            conn
        } ?: getBestHttpsURLConnection(url)
    }

    private fun getBestHttpsURLConnection(url: URL): HttpsURLConnection = NetCipher.getCompatibleHttpsURLConnection(url)
}

