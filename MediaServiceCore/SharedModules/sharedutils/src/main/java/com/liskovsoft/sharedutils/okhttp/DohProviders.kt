package com.liskovsoft.sharedutils.okhttp

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.ArrayList

 
object DohProviders {
    @JvmStatic
    fun buildGoogle(bootstrapClient: OkHttpClient): DnsOverHttps {
        if (cachedGoogle == null) {
            cachedGoogle = DnsOverHttps.Builder().client(bootstrapClient)
                .url(parseUrl("https://dns.google/dns-query"))
                .bootstrapDnsHosts(getByIp("8.8.4.4"), getByIp("8.8.8.8"))
                .build()
        }
        return cachedGoogle!!
    }

    @JvmStatic
    var cachedGoogle: DnsOverHttps? = null
        private set

    @JvmStatic
    fun buildGooglePost(bootstrapClient: OkHttpClient): DnsOverHttps {
        return DnsOverHttps.Builder().client(bootstrapClient)
            .url(parseUrl("https://dns.google/dns-query"))
            .bootstrapDnsHosts(getByIp("8.8.4.4"), getByIp("8.8.8.8"))
            .post(true)
            .build()
    }

    @JvmStatic
    fun buildCloudflare(bootstrapClient: OkHttpClient): DnsOverHttps {
        return DnsOverHttps.Builder().client(bootstrapClient)
            .url(parseUrl("https://1.1.1.1/dns-query?ct=application/dns-udpwireformat"))
            .bootstrapDnsHosts(getByIp("1.1.1.1"), getByIp("1.0.0.1"))
            .includeIPv6(false)
            .build()
    }

    @JvmStatic
    fun buildCloudflarePost(bootstrapClient: OkHttpClient): DnsOverHttps {
        return DnsOverHttps.Builder().client(bootstrapClient)
            .url(parseUrl("https://dns.cloudflare.com/.well-known/dns-query"))
            .includeIPv6(false)
            .post(true)
            .build()
    }

    @JvmStatic
    fun buildCleanBrowsing(bootstrapClient: OkHttpClient): DnsOverHttps {
        return DnsOverHttps.Builder().client(bootstrapClient)
            .url(parseUrl("https://doh.cleanbrowsing.org/doh/family-filter"))
            .includeIPv6(false)
            .build()
    }

    @JvmStatic
    fun providers(client: OkHttpClient, getOnly: Boolean): List<DnsOverHttps> {
        val result = ArrayList<DnsOverHttps>()

        result.add(buildGoogle(client))
        if (!getOnly) {
            result.add(buildGooglePost(client))
        }
        result.add(buildCloudflare(client))
        if (!getOnly) {
            result.add(buildCloudflarePost(client))
        }
        result.add(buildCleanBrowsing(client))

        return result
    }

    internal fun parseUrl(s: String): HttpUrl {

        return s.toHttpUrlOrNull() ?: throw NullPointerException("unable to parse url")
    }

    private fun getByIp(host: String): InetAddress {
        try {
            return InetAddress.getByName(host)
        } catch (e: UnknownHostException) {
             
            throw RuntimeException(e)
        }
    }
}

