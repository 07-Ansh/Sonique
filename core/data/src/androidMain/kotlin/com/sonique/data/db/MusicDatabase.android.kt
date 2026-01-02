package com.sonique.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.sonique.common.DB_NAME
import com.sonique.domain.data.entities.PairSongLocalPlaylist
import com.sonique.domain.data.entities.SetVideoIdEntity
import com.sonique.logger.Logger
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json
import org.koin.mp.KoinPlatform.getKoin
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
actual fun getDatabaseBuilder(converters: Converters) : RoomDatabase.Builder<MusicDatabase> {
    val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }
    return Room
        .databaseBuilder(getKoin().get(), MusicDatabase::class.java, DB_NAME)
        .addTypeConverter(converters)
        .addMigrations(
            object : Migration(5, 6) {
                override fun migrate(connection: SQLiteConnection) {
                    val playlistSongMaps = mutableListOf<PairSongLocalPlaylist>()
                    connection.prepare("SELECT * FROM local_playlist").use { cursor ->
                        while (cursor.step()) {
                            if (!cursor.isNull(8)) {
                                val input = cursor.getText(8)
                                val tracks =
                                    json.decodeFromString<ArrayList<String?>?>(input)
                                Logger.w("MIGRATION_5_6", "tracks: $tracks")
                                tracks?.mapIndexed { index, track ->
                                    if (track != null) {
                                        playlistSongMaps.add(
                                            PairSongLocalPlaylist(
                                                playlistId = cursor.getLong(0),
                                                songId = track,
                                                position = index,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    connection.execSQL("ALTER TABLE `format` ADD COLUMN `lengthSeconds` INTEGER DEFAULT NULL")
                    connection.execSQL("ALTER TABLE `format` ADD COLUMN `youtubeCaptionsUrl` TEXT DEFAULT NULL")
                    connection.execSQL("ALTER TABLE `format` ADD COLUMN `cpn` TEXT DEFAULT NULL")
                    connection.execSQL(
                        "CREATE TABLE IF NOT EXISTS `pair_song_local_playlist` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `playlistId` INTEGER NOT NULL, `songId` TEXT NOT NULL, `position` INTEGER NOT NULL, `inPlaylist` INTEGER NOT NULL, FOREIGN KEY(`playlistId`) REFERENCES `local_playlist`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`songId`) REFERENCES `song`(`videoId`) ON UPDATE NO ACTION ON DELETE CASCADE )",
                    )
                    connection.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_pair_song_local_playlist_playlistId` ON `pair_song_local_playlist` (`playlistId`)",
                    )
                    connection.execSQL("CREATE INDEX IF NOT EXISTS `index_pair_song_local_playlist_songId` ON `pair_song_local_playlist` (`songId`)")
                    playlistSongMaps.forEach { pair ->
                        connection.execSQL(
                            "INSERT OR IGNORE INTO pair_song_local_playlist (playlistId, songId, position, inPlaylist) VALUES (${pair.playlistId}, '${pair.songId}', ${pair.position}, ${pair.inPlaylist.toInstant(TimeZone.UTC).toEpochMilliseconds()})"
                        )
                    }
                }
            },
            object : Migration(10, 11) {
                override fun migrate(connection: SQLiteConnection) {
                    val listYouTubeSyncedId = mutableListOf<Pair<String, List<String>>>() // Pair<youtubePlaylistId, listVideoId>
                    connection
                        .prepare(
                            "SELECT youtubePlaylistId, tracks FROM local_playlist WHERE synced_with_youtube_playlist = 1 AND youtubePlaylistId NOT NULL"
                        ).use { cursor ->
                            while (cursor.step()) {
                                val youtubePlaylistId = cursor.getText(0)
                                val input = cursor.getText(1)
                                val tracks =
                                    json.decodeFromString<ArrayList<String?>?>(input)
                                listYouTubeSyncedId.add(Pair(youtubePlaylistId, tracks?.toMutableList()?.filterNotNull() ?: emptyList()))
                            }
                        }
                    val setVideoIdList = mutableListOf<SetVideoIdEntity>()
                    connection.prepare("SELECT * FROM set_video_id").use { cursor ->
                        while (cursor.step()) {
                            val videoId = cursor.getText(0)
                            val setVideoId = cursor.getText(1)
                            for (pair in listYouTubeSyncedId) {
                                if (pair.second.contains(videoId)) {
                                    setVideoIdList.add(SetVideoIdEntity(videoId, setVideoId, pair.first))
                                    break
                                }
                            }
                        }
                    }
                    connection.execSQL("DROP TABLE set_video_id")
                    connection.execSQL(
                        "CREATE TABLE IF NOT EXISTS `set_video_id` (`videoId` TEXT NOT NULL, `setVideoId` TEXT, `youtubePlaylistId` TEXT NOT NULL, PRIMARY KEY(`videoId`, `youtubePlaylistId`))",
                    )
                    setVideoIdList.forEach { setVideoIdEntity ->
                        connection.execSQL(
                            "INSERT OR IGNORE INTO set_video_id (videoId, setVideoId, youtubePlaylistId) VALUES ('${setVideoIdEntity.videoId}', '${setVideoIdEntity.setVideoId}', '${setVideoIdEntity.youtubePlaylistId}')"
                        )
                    }
                }
            },
            object : Migration(12, 13) {
                override fun migrate(connection: SQLiteConnection) {
                    connection.execSQL("ALTER TABLE song ADD COLUMN canvasUrl TEXT")
                }
            },
        ).addCallback(
            object : RoomDatabase.Callback() {
                override fun onOpen(connection: SQLiteConnection) {
                    super.onOpen(connection)
                    connection.execSQL(
                        "CREATE TRIGGER  IF NOT EXISTS on_delete_pair_song_local_playlist AFTER DELETE ON pair_song_local_playlist\n" +
                            "FOR EACH ROW\n" +
                            "BEGIN\n" +
                            "    UPDATE pair_song_local_playlist\n" +
                            "    SET position = position - 1\n" +
                            "    WHERE playlistId = OLD.playlistId AND position > OLD.position;\n" +
                            "END;",
                    )
                }
            },
        )
}

actual fun getDatabasePath(): String {
    return getKoin().get<Context>().getDatabasePath(DB_NAME).path
}

