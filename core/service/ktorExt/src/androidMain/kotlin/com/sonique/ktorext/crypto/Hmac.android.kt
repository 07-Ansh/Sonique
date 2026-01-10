package com.sonique.ktorext.crypto

import java.time.Instant
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

actual class Hmac actual constructor(algorithm: String, secretKey: String) {
    private var tokenTtl: Long = 300000  
    private val mac: Mac by lazy {
        try {
            Mac.getInstance(algorithm).apply {
                init(SecretKeySpec(secretKey.toByteArray(), algorithm))
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize HMAC", e)
        }
    }

    actual fun getMacTimestampPair(uri: String): Pair<String, String> {
        val timestamp = Instant.now().toEpochMilli().toString()
        val data = "$timestamp$uri"
        val hmac = this.generateHmac(data)
        return hmac to timestamp
    }

     
    actual fun generateHmac(data: String): String = Base64.getEncoder().encodeToString(mac.doFinal(data.toByteArray()))

     
    actual fun validateHmac(
        data: String,
        hmac: String,
    ): Boolean {
        val calculatedHmac = generateHmac(data)
        return calculatedHmac == hmac
    }

     
    actual fun isValidTimestamp(timestamp: String): Boolean {
        val requestTime = timestamp.toLongOrNull() ?: return false
        val currentTime = System.currentTimeMillis()
        return (currentTime - requestTime) < tokenTtl
    }
}

