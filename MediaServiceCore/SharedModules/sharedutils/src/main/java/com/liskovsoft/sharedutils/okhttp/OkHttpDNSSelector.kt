package com.liskovsoft.sharedutils.okhttp

import okhttp3.Dns
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

 
class OkHttpDNSSelector(private val mode: IPvMode) : Dns {

    enum class IPvMode(val code: String) {
        SYSTEM("system"),
        IPV6_FIRST("ipv6"),
        IPV4_FIRST("ipv4"),
        IPV6_ONLY("ipv6only"),
        IPV4_ONLY("ipv4only");

        companion object {
            @JvmStatic
            fun fromString(ipMode: String): IPvMode =
                IPvMode.values().find { it.code == ipMode } ?: throw Exception("Unknown value $ipMode")
        }
    }

    override fun lookup(hostname: String): List<InetAddress> {
        var addresses = Dns.SYSTEM.lookup(hostname)

        addresses = when (mode) {
            IPvMode.IPV6_FIRST -> addresses.sortedBy { Inet4Address::class.java.isInstance(it) }
            IPvMode.IPV4_FIRST -> addresses.sortedBy { Inet6Address::class.java.isInstance(it) }
            IPvMode.IPV6_ONLY -> addresses.filter { Inet6Address::class.java.isInstance(it) }
            IPvMode.IPV4_ONLY -> addresses.filter { Inet4Address::class.java.isInstance(it) }
            IPvMode.SYSTEM -> addresses
        }

         

        return addresses
    }
}

