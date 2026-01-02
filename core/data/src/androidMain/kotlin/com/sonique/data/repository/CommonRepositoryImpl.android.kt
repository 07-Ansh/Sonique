package com.sonique.data.repository

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.OPEN_READONLY
import android.webkit.CookieManager
import com.sonique.domain.data.model.cookie.CookieItem
import java.io.File

actual fun getCookies(url: String, packageName: String): CookieItem {
        return try {
            val projection =
                arrayOf(
                    CookieItem.HOST,
                    CookieItem.EXPIRY,
                    CookieItem.PATH,
                    CookieItem.NAME,
                    CookieItem.VALUE,
                    CookieItem.SECURE,
                )
            CookieManager.getInstance().flush()
            val cookieList = mutableListOf<CookieItem.Content>()
            val dbPath =
                File("/data/data/${packageName}/").walkTopDown().find { it.name == "Cookies" }
                    ?: throw Exception("Cookies File not found!")

            val db =
                SQLiteDatabase.openDatabase(
                    dbPath.absolutePath,
                    null,
                    OPEN_READONLY,
                )
            db
                .query(
                    "cookies",
                    projection,
                    null,
                    null,
                    null,
                    null,
                    null,
                ).run {
                    while (moveToNext()) {
                        val expiry = getLong(getColumnIndexOrThrow(CookieItem.EXPIRY))
                        val name = getString(getColumnIndexOrThrow(CookieItem.NAME))
                        val value = getString(getColumnIndexOrThrow(CookieItem.VALUE))
                        val path = getString(getColumnIndexOrThrow(CookieItem.PATH))
                        val secure = getLong(getColumnIndexOrThrow(CookieItem.SECURE)) == 1L
                        val hostKey = getString(getColumnIndexOrThrow(CookieItem.HOST))

                        val host = if (hostKey[0] != '.') ".$hostKey" else hostKey
                        cookieList.add(
                            CookieItem.Content(
                                domain = host,
                                name = name,
                                value = value,
                                isSecure = secure,
                                expiresUtc = expiry,
                                hostKey = host,
                                path = path,
                            ),
                        )
                    }
                    close()
                }
            db.close()
            CookieItem(url, cookieList)
        } catch (e: Exception) {
            e.printStackTrace()
            CookieItem(url, emptyList())
        }
}

