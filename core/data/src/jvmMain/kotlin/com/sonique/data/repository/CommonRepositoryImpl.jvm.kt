package com.sonique.data.repository

import com.sonique.domain.data.model.cookie.CookieItem

actual fun getCookies(url: String, packageName: String): CookieItem = CookieItem(url, emptyList())

