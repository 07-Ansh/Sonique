package com.sonique.domain.data.model.network

import com.sonique.domain.manager.DataStoreManager

data class ProxyConfiguration(
    val host: String,
    val port: Int,
    val type: DataStoreManager.ProxyType
)

