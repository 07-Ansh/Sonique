package com.sonique.data.db

import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Suppress("ktlint:standard:class-naming")
@DeleteTable(
    tableName = "format",
)
internal class AutoMigration7_8 : AutoMigrationSpec {
    override fun onPostMigrate(connection: SQLiteConnection) {
        super.onPostMigrate(connection)
        connection.execSQL("DROP TABLE IF EXISTS `format`")
    }
}

@Suppress("ktlint:standard:class-naming")
@DeleteColumn(
    tableName = "local_playlist",
    columnName = "synced_with_youtube_playlist",
)
internal class AutoMigration11_12 : AutoMigrationSpec

