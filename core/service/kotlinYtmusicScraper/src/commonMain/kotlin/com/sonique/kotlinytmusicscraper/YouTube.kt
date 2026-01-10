package com.sonique.kotlinytmusicscraper

import com.eygraber.uri.toKmpUri
import com.sonique.kotlinytmusicscraper.YouTube.Companion.DEFAULT_VISITOR_DATA
import com.sonique.kotlinytmusicscraper.extension.toListFormat
import com.sonique.kotlinytmusicscraper.models.AccountInfo
import com.sonique.kotlinytmusicscraper.models.AlbumItem
import com.sonique.kotlinytmusicscraper.models.Artist
import com.sonique.kotlinytmusicscraper.models.ArtistItem
import com.sonique.kotlinytmusicscraper.models.BrowseEndpoint
import com.sonique.kotlinytmusicscraper.models.GridRenderer
import com.sonique.kotlinytmusicscraper.models.MediaType
import com.sonique.kotlinytmusicscraper.models.MusicCarouselShelfRenderer
import com.sonique.kotlinytmusicscraper.models.MusicShelfRenderer
import com.sonique.kotlinytmusicscraper.models.MusicTwoRowItemRenderer
import com.sonique.kotlinytmusicscraper.models.PlaylistItem
import com.sonique.kotlinytmusicscraper.models.ReturnYouTubeDislikeResponse
import com.sonique.kotlinytmusicscraper.models.Run
import com.sonique.kotlinytmusicscraper.models.SearchSuggestions
import com.sonique.kotlinytmusicscraper.models.SongInfo
import com.sonique.kotlinytmusicscraper.models.SongItem
import com.sonique.kotlinytmusicscraper.models.VideoItem
import com.sonique.kotlinytmusicscraper.models.WatchEndpoint
import com.sonique.kotlinytmusicscraper.models.YTItemType
import com.sonique.kotlinytmusicscraper.models.YouTubeClient
import com.sonique.kotlinytmusicscraper.models.YouTubeClient.Companion.TVHTML5
import com.sonique.kotlinytmusicscraper.models.YouTubeClient.Companion.WEB
import com.sonique.kotlinytmusicscraper.models.YouTubeClient.Companion.WEB_REMIX
import com.sonique.kotlinytmusicscraper.models.YouTubeLocale
import com.sonique.kotlinytmusicscraper.models.getContinuation
import com.sonique.kotlinytmusicscraper.models.oddElements
import com.sonique.kotlinytmusicscraper.models.response.AccountMenuResponse
import com.sonique.kotlinytmusicscraper.models.response.AccountSwitcherEndpointResponse
import com.sonique.kotlinytmusicscraper.models.response.AddItemYouTubePlaylistResponse
import com.sonique.kotlinytmusicscraper.models.response.BrowseResponse
import com.sonique.kotlinytmusicscraper.models.response.CreatePlaylistResponse
import com.sonique.kotlinytmusicscraper.models.response.DownloadProgress
import com.sonique.kotlinytmusicscraper.models.response.GetQueueResponse
import com.sonique.kotlinytmusicscraper.models.response.GetSearchSuggestionsResponse
import com.sonique.kotlinytmusicscraper.models.response.LikeStatus
import com.sonique.kotlinytmusicscraper.models.response.NextAndroidMusicResponse
import com.sonique.kotlinytmusicscraper.models.response.NextResponse
import com.sonique.kotlinytmusicscraper.models.response.PipedResponse
import com.sonique.kotlinytmusicscraper.models.response.PlayerResponse
import com.sonique.kotlinytmusicscraper.models.response.SearchResponse
import com.sonique.kotlinytmusicscraper.models.response.toLikeStatus
import com.sonique.kotlinytmusicscraper.models.response.toListAccountInfo
import com.sonique.kotlinytmusicscraper.models.sponsorblock.SkipSegments
import com.sonique.kotlinytmusicscraper.models.youtube.GhostResponse
import com.sonique.kotlinytmusicscraper.models.youtube.Transcript
import com.sonique.kotlinytmusicscraper.models.youtube.YouTubeInitialPage
import com.sonique.kotlinytmusicscraper.models.youtube.tryDecodeText
import com.sonique.kotlinytmusicscraper.pages.AlbumPage
import com.sonique.kotlinytmusicscraper.pages.ArtistPage
import com.sonique.kotlinytmusicscraper.pages.ArtistSection
import com.sonique.kotlinytmusicscraper.pages.BrowseResult
import com.sonique.kotlinytmusicscraper.pages.ExplorePage
import com.sonique.kotlinytmusicscraper.pages.MoodAndGenres
import com.sonique.kotlinytmusicscraper.pages.NextPage
import com.sonique.kotlinytmusicscraper.pages.NextResult
import com.sonique.kotlinytmusicscraper.pages.PlaylistContinuationPage
import com.sonique.kotlinytmusicscraper.pages.PlaylistPage
import com.sonique.kotlinytmusicscraper.pages.RelatedPage
import com.sonique.kotlinytmusicscraper.pages.SearchPage
import com.sonique.kotlinytmusicscraper.pages.SearchResult
import com.sonique.kotlinytmusicscraper.pages.SearchSuggestionPage
import com.sonique.kotlinytmusicscraper.parser.fromPlaylistContinuationToTracks
import com.sonique.kotlinytmusicscraper.parser.fromPlaylistToTrack
import com.sonique.kotlinytmusicscraper.parser.fromPlaylistToTrackWithSetVideoId
import com.sonique.kotlinytmusicscraper.parser.getContinuePlaylistContinuation
import com.sonique.kotlinytmusicscraper.parser.getPlaylistContinuation
import com.sonique.kotlinytmusicscraper.parser.getReloadParams
import com.sonique.kotlinytmusicscraper.parser.getSuggestionSongItems
import com.sonique.kotlinytmusicscraper.parser.hasReloadParams
import com.sonique.logger.Logger
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlHandler
import com.mohamedrejeb.ksoup.html.parser.KsoupHtmlParser
import io.ktor.client.call.body
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.http
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.parseQueryString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okio.Path
import kotlin.jvm.JvmInline
import kotlin.math.round
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val TAG = "YouTubeScraper"

 
class YouTube {
    private val ytMusic = Ytmusic()

    var cookiePath: Path?
        get() = ytMusic.cookiePath
        set(value) {
            ytMusic.cookiePath = value
        }

     
    var locale: YouTubeLocale
        get() = ytMusic.locale
        set(value) {
            ytMusic.locale = value
        }

     
    var visitorData: String?
        get() = ytMusic.visitorData
        set(value) {
            ytMusic.visitorData = value
        }

    var dataSyncId: String?
        get() = ytMusic.dataSyncId
        set(value) {
            ytMusic.dataSyncId = value
        }

     
    var cookie: String?
        get() = ytMusic.cookie
        set(value) {
            ytMusic.cookie = value
        }

    var pageId: String?
        get() = ytMusic.pageId
        set(value) {
            ytMusic.pageId = value
        }

     
    private val poTokenJsonDeserializer =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            coerceInputValues = true
            useArrayPolymorphism = true
        }

    private fun String.getPoToken(): String? =
        this
            .replace("[", "")
            .replace("]", "")
            .split(",")
            .findLast { it.contains("\"") }
            ?.replace("\"", "")

    private var poTokenObject: Pair<String?, Long> = Pair(null, 0)

     
    fun removeProxy() {
        ytMusic.proxy = null
    }

     
    fun setProxy(
        isHttp: Boolean,
        host: String,
        port: Int,
    ) {
        runCatching {
            if (isHttp) ProxyBuilder.http("$host:$port") else ProxyBuilder.socks(host, port)
        }.onSuccess {
            ytMusic.proxy = it
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun updateYtdlp() {
        runCatching {
            ytMusic.updateYtdlp()
        }.onFailure {
            it.printStackTrace()
        }
    }

    private val listPipedInstances =
        listOf(
            "https://pipedapi.nosebs.ru",
            "https://pipedapi.kavin.rocks",
            "https://pipedapi.tokhmi.xyz",
            "https://pipedapi.syncpundit.io",
            "https://pipedapi.leptons.xyz",
            "https://pipedapi.r4fo.com",
            "https://yapi.vyper.me",
            "https://pipedapi-libre.kavin.rocks",
        )

     
    suspend fun search(
        query: String,
        filter: SearchFilter,
    ): Result<SearchResult> =
        runCatching {
            val response = ytMusic.search(WEB_REMIX, query, filter.value).body<SearchResponse>()
            SearchResult(
                items =
                    response.contents
                        ?.tabbedSearchResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.lastOrNull()
                        ?.musicShelfRenderer
                        ?.contents
                        ?.mapNotNull {
                            SearchPage.toYTItem(it.musicResponsiveListItemRenderer)
                        }.orEmpty(),
                listPodcast =
                    response.contents
                        ?.tabbedSearchResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.lastOrNull()
                        ?.musicShelfRenderer
                        ?.contents
                        ?.mapNotNull {
                            SearchPage.toPodcast(it.musicResponsiveListItemRenderer)
                        }.orEmpty(),
                continuation =
                    response.contents
                        ?.tabbedSearchResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.lastOrNull()
                        ?.musicShelfRenderer
                        ?.continuations
                        ?.getContinuation(),
            )
        }

     
    suspend fun searchContinuation(continuation: String): Result<SearchResult> =
        runCatching {
            val response = ytMusic.search(WEB_REMIX, continuation = continuation).body<SearchResponse>()
            SearchResult(
                items =
                    response.continuationContents
                        ?.musicShelfContinuation
                        ?.contents
                        ?.mapNotNull {
                            SearchPage.toYTItem(it.musicResponsiveListItemRenderer)
                        }!!,
                listPodcast =
                    response.continuationContents.musicShelfContinuation.contents
                        .mapNotNull {
                            SearchPage.toPodcast(it.musicResponsiveListItemRenderer)
                        }.orEmpty(),
                continuation =
                    response.continuationContents.musicShelfContinuation.continuations
                        ?.getContinuation(),
            )
        }

     
    suspend fun album(
        browseId: String,
        withSongs: Boolean = true,
    ): Result<AlbumPage> =
        runCatching {
            val response = ytMusic.browse(WEB_REMIX, browseId).body<BrowseResponse>()
            val playlistId =
                response.microformat
                    ?.microformatDataRenderer
                    ?.urlCanonical
                    ?.substringAfterLast('=')!!
            val albumItem =
                AlbumItem(
                    browseId = browseId,
                    playlistId = playlistId,
                    title =
                        response.contents
                            ?.twoColumnBrowseResultsRenderer
                            ?.tabs
                            ?.firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                            ?.musicResponsiveHeaderRenderer
                            ?.title
                            ?.runs
                            ?.firstOrNull()
                            ?.text ?: "",
                    artists =
                        response.contents
                            ?.twoColumnBrowseResultsRenderer
                            ?.tabs
                            ?.firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                            ?.musicResponsiveHeaderRenderer
                            ?.straplineTextOne
                            ?.runs
                            ?.oddElements()
                            ?.map {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                )
                            }!!,
                    year =
                        response.contents.twoColumnBrowseResultsRenderer.tabs
                            .firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                            ?.musicResponsiveHeaderRenderer
                            ?.subtitle
                            ?.runs
                            ?.lastOrNull()
                            ?.text
                            ?.toIntOrNull(),
                    thumbnail =
                        response.contents.twoColumnBrowseResultsRenderer.tabs
                            .firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                            ?.musicResponsiveHeaderRenderer
                            ?.thumbnail
                            ?.musicThumbnailRenderer
                            ?.getThumbnailUrl()!!,
                )
            AlbumPage(
                album = albumItem,
                songs =
                    if (withSongs) {
                        albumSongs(
                            response.contents
                                .twoColumnBrowseResultsRenderer
                                .secondaryContents
                                ?.sectionListRenderer
                                ?.contents
                                ?.firstOrNull()
                                ?.musicShelfRenderer
                                ?.contents,
                            albumItem,
                        ).getOrThrow()
                    } else {
                        emptyList()
                    },
                description =
                    getDescriptionAlbum(
                        response.contents.twoColumnBrowseResultsRenderer.tabs
                            .firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                            ?.musicResponsiveHeaderRenderer
                            ?.description
                            ?.musicDescriptionShelfRenderer
                            ?.description
                            ?.runs,
                    ),
                duration =
                    response.contents.twoColumnBrowseResultsRenderer.tabs
                        .firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.firstOrNull()
                        ?.musicResponsiveHeaderRenderer
                        ?.secondSubtitle
                        ?.runs
                        ?.get(2)
                        ?.text ?: "",
                thumbnails =
                    response.contents.twoColumnBrowseResultsRenderer.tabs
                        .firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.firstOrNull()
                        ?.musicResponsiveHeaderRenderer
                        ?.thumbnail
                        ?.musicThumbnailRenderer
                        ?.thumbnail,
                otherVersion =
                    response.contents
                        .twoColumnBrowseResultsRenderer
                        .secondaryContents
                        ?.sectionListRenderer
                        ?.contents
                        ?.lastOrNull()
                        ?.musicCarouselShelfRenderer
                        ?.contents
                        ?.mapNotNull {
                            AlbumPage.fromMusicTwoRowItemRenderer(
                                it.musicTwoRowItemRenderer,
                            )
                        } ?: emptyList(),
            )
        }

    private fun getDescriptionAlbum(runs: List<Run>?): String {
        var description = ""
        if (!runs.isNullOrEmpty()) {
            for (run in runs) {
                description += run.text
            }
        }
        Logger.d("description", description)
        return description
    }

    private fun albumSongs(
        content: List<MusicShelfRenderer.Content>?,
        album: AlbumItem,
    ): Result<List<SongItem>> =
        runCatching {
            if (content == null) {
                return@runCatching emptyList()
            } else {
                return@runCatching content.mapNotNull {
                    AlbumPage.fromMusicResponsiveListItemRenderer(it.musicResponsiveListItemRenderer, album)
                }
            }
        }

    suspend fun testArtist(browseId: String): Result<ArrayList<ArtistSection>> =
        runCatching {
            val response = ytMusic.browse(WEB_REMIX, browseId).body<BrowseResponse>()
            val artistSections = arrayListOf<ArtistSection>()
            val content =
                response.contents
                    ?.singleColumnBrowseResultsRenderer
                    ?.tabs
                    ?.firstOrNull()
                    ?.tabRenderer
                    ?.content
                    ?.sectionListRenderer
                    ?.contents
            if (content != null) {
                for (i in 0 until content.size) {
                    ArtistPage
                        .fromSectionListRendererContent(content.get(i))
                        ?.let { artistSections.add(it) }
                    Logger.d(TAG, "Section $i checking \n artistSection ${artistSections.lastOrNull()}")
                }
            }
            return@runCatching artistSections
        }

     
    suspend fun artist(browseId: String): Result<ArtistPage> =
        runCatching {
            val response = ytMusic.browse(WEB_REMIX, browseId).body<BrowseResponse>()
            ArtistPage(
                artist =
                    ArtistItem(
                        id = browseId,
                        title =
                            response.header
                                ?.musicImmersiveHeaderRenderer
                                ?.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text
                                ?: response.header
                                    ?.musicVisualHeaderRenderer
                                    ?.title
                                    ?.runs
                                    ?.firstOrNull()
                                    ?.text!!,
                        thumbnail =
                            response.header
                                ?.musicImmersiveHeaderRenderer
                                ?.thumbnail
                                ?.musicThumbnailRenderer
                                ?.getThumbnailUrl()
                                ?: response.header
                                    ?.musicVisualHeaderRenderer
                                    ?.foregroundThumbnail
                                    ?.musicThumbnailRenderer
                                    ?.getThumbnailUrl()!!,
                        shuffleEndpoint =
                            response.header
                                ?.musicImmersiveHeaderRenderer
                                ?.playButton
                                ?.buttonRenderer
                                ?.navigationEndpoint
                                ?.watchEndpoint,
                        radioEndpoint =
                            response.header
                                ?.musicImmersiveHeaderRenderer
                                ?.startRadioButton
                                ?.buttonRenderer
                                ?.navigationEndpoint
                                ?.watchEndpoint,
                    ),
                sections =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.mapNotNull(ArtistPage::fromSectionListRendererContent)!!,
                description =
                    response.header
                        ?.musicImmersiveHeaderRenderer
                        ?.description
                        ?.runs
                        ?.firstOrNull()
                        ?.text,
                subscribers =
                    response.header
                        ?.musicImmersiveHeaderRenderer
                        ?.subscriptionButton
                        ?.subscribeButtonRenderer
                        ?.longSubscriberCountText
                        ?.runs
                        ?.get(
                            0,
                        )?.text,
                view =
                    response.contents.singleColumnBrowseResultsRenderer.tabs[0]
                        .tabRenderer.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.lastOrNull()
                        ?.musicDescriptionShelfRenderer
                        ?.subheader
                        ?.runs
                        ?.firstOrNull()
                        ?.text,
            )
        }

    suspend fun getYouTubePlaylistFullTracksWithSetVideoId(playlistId: String): Result<List<Pair<SongItem, String>>> =
        runCatching {
            val plId = if (playlistId.startsWith("VL")) playlistId else "VL$playlistId"
             
            val listPair = mutableListOf<Pair<SongItem, String>>()
            val response = ytMusic.playlist(plId).body<BrowseResponse>()
            listPair.addAll(
                response.fromPlaylistToTrackWithSetVideoId(),
            )
            var continuation = response.getPlaylistContinuation()
            while (continuation != null) {
                val continuationResponse =
                    ytMusic
                        .browse(
                            client = WEB_REMIX,
                            setLogin = true,
                            params = null,
                            continuation = continuation,
                        ).body<BrowseResponse>()
                listPair.addAll(
                    continuationResponse.fromPlaylistToTrackWithSetVideoId(),
                )
                continuation = continuationResponse.getContinuePlaylistContinuation()
            }

            return@runCatching listPair
        }

    suspend fun getSuggestionsTrackForPlaylist(playlistId: String): Result<Pair<String?, List<SongItem>?>?> =
        runCatching {
            val initialResponse =
                ytMusic
                    .playlist(
                        if (playlistId.startsWith("VL")) playlistId else "VL$playlistId",
                    ).body<BrowseResponse>()
            var continuation = initialResponse.getPlaylistContinuation()
            Logger.d(TAG, "YouTube: getSuggestionsTrackForPlaylist: $continuation")
            while (continuation != null) {
                val continuationResponse =
                    ytMusic
                        .browse(
                            client = WEB_REMIX,
                            setLogin = true,
                            params = "wAEB",
                            continuation = continuation,
                        ).body<BrowseResponse>()
                Logger.d(TAG, "YouTube: getSuggestionsTrackForPlaylist: ${continuationResponse.getReloadParams()}")
                if (continuationResponse.hasReloadParams()) {
                    return@runCatching Pair(continuationResponse.getReloadParams(), continuationResponse.getSuggestionSongItems())
                } else {
                    continuation = continuationResponse.getContinuePlaylistContinuation()
                }
            }
            return@runCatching null
        }

    suspend fun getPlaylistFullTracks(playlistId: String): Result<List<SongItem>> =
        runCatching {
            val songs = mutableListOf<SongItem>()
            val response = ytMusic.playlist(playlistId).body<BrowseResponse>()
            songs.addAll(
                response.fromPlaylistToTrack(),
            )
            var continuation = response.getPlaylistContinuation()
            while (continuation != null) {
                val continuationResponse =
                    ytMusic
                        .browse(
                            client = WEB_REMIX,
                            setLogin = true,
                            params = null,
                            continuation = continuation,
                        ).body<BrowseResponse>()
                songs.addAll(
                    continuationResponse.fromPlaylistContinuationToTracks(),
                )
                continuation = continuationResponse.getContinuePlaylistContinuation()
            }
            return@runCatching songs
        }

     
    suspend fun playlist(playlistId: String): Result<PlaylistPage> =
        runCatching {
            val response =
                ytMusic
                    .browse(
                        client = WEB_REMIX,
                        browseId = "VL$playlistId",
                        setLogin = true,
                    ).body<BrowseResponse>()
            val header =
                response.header?.musicDetailHeaderRenderer
                    ?: response.header
                        ?.musicEditablePlaylistDetailHeaderRenderer
                        ?.header
                        ?.musicDetailHeaderRenderer!!
            PlaylistPage(
                playlist =
                    PlaylistItem(
                        id = playlistId,
                        title =
                            header.title.runs
                                ?.firstOrNull()
                                ?.text!!,
                        author =
                            header.subtitle.runs?.getOrNull(2)?.let {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                )
                            },
                        songCountText =
                            header.secondSubtitle.runs
                                ?.firstOrNull()
                                ?.text,
                        thumbnail = header.thumbnail.croppedSquareThumbnailRenderer?.getThumbnailUrl()!!,
                        playEndpoint = null,
                        shuffleEndpoint =
                            header.menu.menuRenderer.topLevelButtons
                                ?.firstOrNull()
                                ?.buttonRenderer
                                ?.navigationEndpoint
                                ?.watchPlaylistEndpoint!!,
                        radioEndpoint =
                            header.menu.menuRenderer.items
                                .find {
                                    it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                                }?.menuNavigationItemRenderer
                                ?.navigationEndpoint
                                ?.watchPlaylistEndpoint!!,
                    ),
                songs =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.firstOrNull()
                        ?.musicPlaylistShelfRenderer
                        ?.contents
                        ?.mapNotNull {
                            PlaylistPage.fromMusicResponsiveListItemRenderer(it.musicResponsiveListItemRenderer)
                        }!!,
                songsContinuation =
                    response.contents.singleColumnBrowseResultsRenderer.tabs
                        .firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.firstOrNull()
                        ?.musicPlaylistShelfRenderer
                        ?.continuations
                        ?.getContinuation(),
                continuation =
                    response.contents.singleColumnBrowseResultsRenderer.tabs
                        .firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.continuations
                        ?.getContinuation(),
            )
        }

    suspend fun playlistContinuation(continuation: String) =
        runCatching {
            val response =
                ytMusic
                    .browse(
                        client = WEB_REMIX,
                        continuation = continuation,
                        setLogin = true,
                    ).body<BrowseResponse>()
            PlaylistContinuationPage(
                songs =
                    response.continuationContents?.musicPlaylistShelfContinuation?.contents?.mapNotNull {
                        PlaylistPage.fromMusicResponsiveListItemRenderer(it.musicResponsiveListItemRenderer)
                    }!!,
                continuation =
                    response.continuationContents.musicPlaylistShelfContinuation.continuations
                        ?.getContinuation(),
            )
        }

     
    suspend fun customQuery(
        browseId: String?,
        params: String? = null,
        continuation: String? = null,
        country: String? = null,
        setLogin: Boolean = true,
    ) = runCatching {
        ytMusic.browse(WEB_REMIX, browseId, params, continuation, country, setLogin).body<BrowseResponse>()
    }

     
    suspend fun nextCustom(videoId: String) =
        runCatching {
            ytMusic.nextCustom(WEB_REMIX, videoId).body<NextResponse>()
        }

     
    suspend fun getSkipSegments(videoId: String) =
        runCatching {
            ytMusic.getSkipSegments(videoId).body<List<SkipSegments>>()
        }





    suspend fun newRelease(): Result<ExplorePage> =
        runCatching {
            val response =
                ytMusic.browse(WEB_REMIX, browseId = "FEmusic_new_releases").body<BrowseResponse>()
            ExplorePage(
                released =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.firstOrNull()
                        ?.gridRenderer
                        ?.items
                        ?.mapNotNull { it.musicTwoRowItemRenderer }
                        ?.mapNotNull(RelatedPage::fromMusicTwoRowItemRenderer)
                        .orEmpty()
                        .mapNotNull {
                            if (it.type == YTItemType.PLAYLIST) it as? PlaylistItem else null
                        },
                musicVideo =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.lastOrNull()
                        ?.musicCarouselShelfRenderer
                        ?.contents
                        ?.mapNotNull {
                            it.musicTwoRowItemRenderer
                        }?.mapNotNull(
                            ArtistPage::fromMusicTwoRowItemRenderer,
                        ).orEmpty()
                        .mapNotNull {
                            if (it.type == YTItemType.VIDEO) it as? VideoItem else null
                        },
            )
        }

    suspend fun moodAndGenres(): Result<List<MoodAndGenres>> =
        runCatching {
            val response = ytMusic.browse(WEB_REMIX, browseId = "FEmusic_moods_and_genres").body<BrowseResponse>()
            response.contents
                ?.singleColumnBrowseResultsRenderer
                ?.tabs
                ?.firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents!!
                .mapNotNull(MoodAndGenres.Companion::fromSectionListRendererContent)
        }

    suspend fun browse(
        browseId: String,
        params: String?,
    ): Result<BrowseResult> =
        runCatching {
            val response = ytMusic.browse(WEB_REMIX, browseId = browseId, params = params).body<BrowseResponse>()
            BrowseResult(
                title =
                    response.header
                        ?.musicHeaderRenderer
                        ?.title
                        ?.runs
                        ?.firstOrNull()
                        ?.text,
                items =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.mapNotNull { content ->
                            when {
                                content.gridRenderer != null -> {
                                    BrowseResult.Item(
                                        title =
                                            content.gridRenderer.header
                                                ?.gridHeaderRenderer
                                                ?.title
                                                ?.runs
                                                ?.firstOrNull()
                                                ?.text,
                                        items =
                                            content.gridRenderer.items
                                                .mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer)
                                                .mapNotNull(RelatedPage.Companion::fromMusicTwoRowItemRenderer),
                                    )
                                }

                                content.musicCarouselShelfRenderer != null -> {
                                    BrowseResult.Item(
                                        title =
                                            content.musicCarouselShelfRenderer.header
                                                ?.musicCarouselShelfBasicHeaderRenderer
                                                ?.title
                                                ?.runs
                                                ?.firstOrNull()
                                                ?.text,
                                        items =
                                            content.musicCarouselShelfRenderer.contents
                                                .mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                                                .mapNotNull(RelatedPage.Companion::fromMusicTwoRowItemRenderer),
                                    )
                                }

                                else -> null
                            }
                        }.orEmpty(),
            )
        }

    suspend fun getFullMetadata(videoId: String): Result<YouTubeInitialPage> =
        runCatching {
            val ytScrape = ytMusic.scrapeYouTube(videoId).body<String>()
            var response = ""
            val ksoupHtmlParser =
                KsoupHtmlParser(
                    object : KsoupHtmlHandler {
                        override fun onText(text: String) {
                            super.onText(text)
                            if (text.contains("var ytInitialPlayerResponse")) {
                                val temp = text.replace("var ytInitialPlayerResponse = ", "").dropLast(1)
                                Logger.d("Scrape", "Temp $temp")
                                response = temp.trimIndent()
                            }
                        }
                    },
                )
            ksoupHtmlParser.write(ytScrape)
            ksoupHtmlParser.end()
            val json = Json { ignoreUnknownKeys = true }
            return@runCatching json.decodeFromString<YouTubeInitialPage>(response)
        }

    suspend fun getLikedInfo(videoId: String): Result<LikeStatus> =
        runCatching {
            val response =
                ytMusic
                    .next(
                        WEB_REMIX,
                        videoId,
                        null,
                        null,
                        null,
                        null,
                        null,
                    ).body<NextAndroidMusicResponse>()
            val likeStatus =
                response.playerOverlays
                    ?.playerOverlayRenderer
                    ?.actions
                    ?.find { it.likeButtonRenderer != null }
                    ?.likeButtonRenderer
                    ?.likeStatus
                    ?.toLikeStatus()
            Logger.w("YouTube", "Like Status ${response.playerOverlays}")
            return@runCatching likeStatus ?: LikeStatus.INDIFFERENT
        }

    suspend fun getSongInfo(videoId: String): Result<SongInfo> =
        runCatching {
            val ytNext = ytMusic.next(WEB, videoId, null, null, null, null, null).body<NextResponse>()
            val videoSecondary =
                ytNext.contents.twoColumnWatchNextResults
                    ?.results
                    ?.results
                    ?.content
                    ?.find {
                        it?.videoSecondaryInfoRenderer != null
                    }?.videoSecondaryInfoRenderer
            val videoPrimary =
                ytNext.contents.twoColumnWatchNextResults
                    ?.results
                    ?.results
                    ?.content
                    ?.find {
                        it?.videoPrimaryInfoRenderer != null
                    }?.videoPrimaryInfoRenderer
            val returnYouTubeDislikeResponse =
                ytMusic.returnYouTubeDislike(videoId).body<ReturnYouTubeDislikeResponse>()
            return@runCatching SongInfo(
                videoId = videoId,
                author =
                    videoSecondary?.owner?.videoOwnerRenderer?.title?.runs?.firstOrNull()?.text?.replace(
                        Regex(" - Topic| - Chủ đề|"),
                        "",
                    ),
                authorId =
                    videoSecondary
                        ?.owner
                        ?.videoOwnerRenderer
                        ?.navigationEndpoint
                        ?.browseEndpoint
                        ?.browseId,
                authorThumbnail =
                    videoSecondary
                        ?.owner
                        ?.videoOwnerRenderer
                        ?.thumbnail
                        ?.thumbnails
                        ?.find {
                            it.height == 48
                        }?.url
                        ?.replace("s48", "s960"),
                description = videoSecondary?.attributedDescription?.content,
                subscribers =
                    videoSecondary
                        ?.owner
                        ?.videoOwnerRenderer
                        ?.subscriberCountText
                        ?.simpleText,
                uploadDate = videoPrimary?.dateText?.simpleText,
                viewCount = returnYouTubeDislikeResponse.viewCount,
                like = returnYouTubeDislikeResponse.likes,
                dislike = returnYouTubeDislikeResponse.dislikes,
            )
             
        }

    private suspend fun getVisitorData(
        videoId: String,
        playlistId: String?,
    ): Triple<String, String, PlayerResponse.PlaybackTracking?> {
        try {
            val pId = if (playlistId?.startsWith("VL") == true) playlistId.removeRange(0..1) else playlistId
            val ghostRequest = ytMusic.ghostRequest(videoId, pId)
            val cookie =
                "PREF=hl=en&tz=UTC; SOCS=CAI; ${
                    ghostRequest.headers
                        .getAll("set-cookie")
                        ?.map {
                            it.split(";").first()
                        }?.filter {
                            it.lastOrNull() != '='
                        }?.joinToString("; ")
                }"
            var response = ""
            var data = ""
            val ksoupHtmlParser =
                KsoupHtmlParser(
                    object : KsoupHtmlHandler {
                        override fun onText(text: String) {
                            super.onText(text)
                            if (text.contains("var ytInitialPlayerResponse")) {
                                val temp = text.replace("var ytInitialPlayerResponse = ", "").split(";var").firstOrNull()
                                temp?.let {
                                    response = it.trimIndent()
                                }
                            } else if (text.contains("var ytInitialData = ")) {
                                val temp = text.replace("var ytInitialData = ", "").dropLast(1)
                                temp.let {
                                    data = it.trimIndent()
                                }
                            }
                        }
                    },
                )
            ksoupHtmlParser.write(ghostRequest.bodyAsText())
            ksoupHtmlParser.end()
            val ytInitialData = poTokenJsonDeserializer.decodeFromString<GhostResponse>(data)
            val ytInitialPlayerResponse = poTokenJsonDeserializer.decodeFromString<GhostResponse>(response)
            val playbackTracking = ytInitialPlayerResponse.playbackTracking
            val loggedIn =
                ytInitialData.responseContext.serviceTrackingParams
                    ?.find { it.service == "GFEEDBACK" }
                    ?.params
                    ?.find { it.key == "logged_in" }
                    ?.value == "1"
            Logger.d(TAG, "Logged In $loggedIn")
            val visitorData =
                ytInitialPlayerResponse.responseContext.serviceTrackingParams
                    ?.find { it.service == "GFEEDBACK" }
                    ?.params
                    ?.find { it.key == "visitor_data" }
                    ?.value
                    ?: ytInitialData.responseContext.webResponseContextExtensionData
                        ?.ytConfigData
                        ?.visitorData
            Logger.d(TAG, "Visitor Data $visitorData")
            Logger.d(TAG, "New Cookie $cookie")
            Logger.d(TAG, "Playback Tracking $playbackTracking")
            return Triple(cookie, visitorData ?: this@YouTube.visitorData ?: "", playbackTracking)
        } catch (e: Exception) {
            e.printStackTrace()
            return Triple("", "", null)
        }
    }

    suspend fun ytDlpPlayer(
        videoId: String,
        tempRes: PlayerResponse,
        useCookie: Boolean = false,
    ): PlayerResponse? {
        val listUrlSig = mutableListOf<String>()
        var decodedSigResponse: PlayerResponse?
        var sigResponse: PlayerResponse?
        Logger.d(TAG, "YouTube TempRes ${tempRes.playabilityStatus}")
        if (tempRes.playabilityStatus.status != "OK") {
            return null
        } else {
            sigResponse = tempRes
        }
        val streamInfo = ytMusic.ytdlpGetStreamUrl(videoId, null, "tv", poTokenJsonDeserializer, useCookie) ?: return null
        val streamsList = streamInfo.formats.takeIf { !it.isNullOrEmpty() } ?: return null
        streamsList.forEach {
            Logger.d(TAG, "YouTube Ytdlp Stream ${it?.formatId} ${it?.url}")
        }
        decodedSigResponse =
            sigResponse.copy(
                streamingData =
                    sigResponse.streamingData?.copy(
                        formats =
                            sigResponse.streamingData.formats?.map { format ->
                                format.copy(
                                    url = streamsList.find { it?.formatId == format.itag.toString() }?.url,
                                )
                            },
                        adaptiveFormats =
                            sigResponse.streamingData.adaptiveFormats.map { adaptiveFormats ->
                                adaptiveFormats.copy(
                                    url = streamsList.find { it?.formatId == adaptiveFormats.itag.toString() }?.url,
                                )
                            },
                    ),
            )
        decodedSigResponse =
            decodedSigResponse.copy(
                streamingData =
                    decodedSigResponse.streamingData?.copy(
                        formats =
                            decodedSigResponse.streamingData.formats?.let { formats ->
                                val copy = formats.toMutableList()
                                streamInfo.formats
                                    ?.filterNotNull()
                                    ?.filter {
                                        isManifestUrl(it.url ?: "")
                                    }?.forEach { manifest ->
                                        copy.add(
                                            PlayerResponse.StreamingData.Format(
                                                itag = manifest.formatId?.toInt() ?: 0,
                                                url = manifest.url,
                                                mimeType = "",
                                                bitrate = 0,
                                                width = manifest.width?.toInt(),
                                                height = manifest.height?.toInt(),
                                                contentLength = 0,
                                                quality = "",
                                                fps = 0,
                                                qualityLabel = "",
                                                averageBitrate = 0,
                                                audioQuality = "",
                                                approxDurationMs = "",
                                                audioSampleRate = 0,
                                                audioChannels = 0,
                                                loudnessDb = 0.0,
                                                lastModified = 0,
                                                signatureCipher = "",
                                            ),
                                        )
                                    }
                                copy.filter { it.itag != 0 }
                                copy
                            },
                    ),
            )
        listUrlSig.addAll(
            (
                decodedSigResponse
                    .streamingData
                    ?.adaptiveFormats
                    ?.mapNotNull { it.url }
                    ?.toMutableList() ?: mutableListOf()
            ).apply {
                decodedSigResponse
                    .streamingData
                    ?.formats
                    ?.mapNotNull { it.url }
                    ?.let { addAll(it) }
            },
        )
        listUrlSig.forEach {
            Logger.d(TAG, "YouTube Ytdlp URL $it")
        }
        val randomUrl = listUrlSig.randomOrNull() ?: return null
        if (listUrlSig.isNotEmpty() && !is403Url(randomUrl)) {
            Logger.d(TAG, "YouTube Ytdlp Found URL $randomUrl")
            return decodedSigResponse
        } else {
            Logger.d(TAG, "YouTube Ytdlp No URL Found")
            return null
        }
    }

    suspend fun smartTubePlayer(
        videoId: String,
        tempRes: PlayerResponse,
    ): PlayerResponse? {
        val listUrlSig = mutableListOf<String>()
        var decodedSigResponse: PlayerResponse?
        var sigResponse: PlayerResponse?
        listUrlSig.removeAll(listUrlSig)
        Logger.d(TAG, "YouTube TempRes ${tempRes.playabilityStatus}")
        if (tempRes.playabilityStatus.status != "OK") {
            return null
        } else {
            sigResponse = tempRes
        }
        val streamsList = ytMusic.getSmartTubePlayer(videoId)
        streamsList.forEach {
            Logger.d(TAG, "YouTube SmartTube Audio Stream ${it.first} ${it.second}")
        }

        if (streamsList.isEmpty()) return null

        decodedSigResponse =
            sigResponse.copy(
                streamingData =
                    sigResponse.streamingData?.copy(
                        formats =
                            sigResponse.streamingData.formats?.map { format ->
                                format.copy(
                                    url = streamsList.find { it.first == format.itag }?.second,
                                )
                            },
                        adaptiveFormats =
                            sigResponse.streamingData.adaptiveFormats.map { adaptiveFormats ->
                                adaptiveFormats.copy(
                                    url = streamsList.find { it.first == adaptiveFormats.itag }?.second,
                                )
                            },
                    ),
            )
        listUrlSig.addAll(
            (
                decodedSigResponse
                    .streamingData
                    ?.adaptiveFormats
                    ?.mapNotNull { it.url }
                    ?.toMutableList() ?: mutableListOf()
            ).apply {
                decodedSigResponse
                    .streamingData
                    ?.formats
                    ?.mapNotNull { it.url }
                    ?.let { addAll(it) }
            },
        )
        Logger.d(TAG, "YouTube URL ${decodedSigResponse.streamingData?.formats?.mapNotNull { it.url }}")
        val listFormat =
            (
                decodedSigResponse
                    .streamingData
                    ?.formats
                    ?.map { Pair(it.itag, it.url) }
                    ?.toMutableList() ?: mutableListOf()
            ).apply {
                addAll(
                    decodedSigResponse.streamingData?.adaptiveFormats?.map {
                        Pair(it.itag, it.url)
                    } ?: emptyList(),
                )
            }
        listFormat.forEach {
            Logger.d(TAG, "YouTube Format ${it.first} ${it.second}")
        }
        val randomUrl = listUrlSig.randomOrNull() ?: return null
        if (listUrlSig.isNotEmpty() && !is403Url(randomUrl)) {
            Logger.d(TAG, "YouTube SmartTube Found URL $randomUrl")
            return decodedSigResponse
        } else {
            Logger.d(TAG, "YouTube SmartTube No URL Found")
            return null
        }
    }

    suspend fun newPipePlayer(
        videoId: String,
        tempRes: PlayerResponse,
    ): PlayerResponse? {
        val listUrlSig = mutableListOf<String>()
        var decodedSigResponse: PlayerResponse?
        var sigResponse: PlayerResponse?
        Logger.d(TAG, "YouTube TempRes ${tempRes.playabilityStatus}")
        if (tempRes.playabilityStatus.status != "OK") {
            return null
        } else {
            sigResponse = tempRes
        }
        val streamsList = ytMusic.getNewPipePlayer(videoId)
        if (streamsList.isEmpty()) return null

        decodedSigResponse =
            sigResponse.copy(
                streamingData =
                    sigResponse.streamingData?.copy(
                        formats =
                            sigResponse.streamingData.formats?.map { format ->
                                format.copy(
                                    url = streamsList.find { it.first == format.itag }?.second,
                                )
                            },
                        adaptiveFormats =
                            sigResponse.streamingData.adaptiveFormats.map { adaptiveFormats ->
                                adaptiveFormats.copy(
                                    url = streamsList.find { it.first == adaptiveFormats.itag }?.second,
                                )
                            },
                        hlsManifestUrl = streamsList.firstOrNull { it.first == 96 }?.second,
                    ),
            )
        decodedSigResponse =
            decodedSigResponse.copy(
                streamingData =
                    decodedSigResponse.streamingData?.copy(
                        formats =
                            decodedSigResponse.streamingData.formats?.let { formats ->
                                val copy = formats.toMutableList()
                                streamsList
                                    .filter {
                                        isManifestUrl(it.second)
                                    }.forEach { manifest ->
                                        copy.add(
                                            PlayerResponse.StreamingData.Format(
                                                itag = manifest.first,
                                                url = manifest.second,
                                                mimeType = "",
                                                bitrate = 0,
                                                width = if (manifest.first == 96) 1920 else 1080,
                                                height = if (manifest.first == 96) 1080 else 720,
                                                contentLength = 0,
                                                quality = "",
                                                fps = 0,
                                                qualityLabel = "",
                                                averageBitrate = 0,
                                                audioQuality = "",
                                                approxDurationMs = "",
                                                audioSampleRate = 0,
                                                audioChannels = 0,
                                                loudnessDb = 0.0,
                                                lastModified = 0,
                                                signatureCipher = "",
                                            ),
                                        )
                                    }
                                copy.filter { it.itag != 0 }
                                copy
                            },
                    ),
            )
        listUrlSig.addAll(
            (
                decodedSigResponse
                    .streamingData
                    ?.adaptiveFormats
                    ?.mapNotNull { it.url }
                    ?.toMutableList() ?: mutableListOf()
            ).apply {
                decodedSigResponse
                    .streamingData
                    ?.formats
                    ?.mapNotNull { it.url }
                    ?.let { addAll(it) }
            },
        )
        listUrlSig.forEach {
            Logger.d(TAG, "YouTube NewPipe URL $it")
        }
        val randomUrl = listUrlSig.randomOrNull() ?: return null
        if (listUrlSig.isNotEmpty() && !is403Url(randomUrl)) {
            Logger.d(TAG, "YouTube NewPipe Found URL $randomUrl")
            return decodedSigResponse
        } else {
            Logger.d(TAG, "YouTube NewPipe No URL Found")
            return null
        }
    }

    fun isManifestUrl(url: String): Boolean = url.contains(".m3u8") || url.contains(".mpd") || url.contains("manifest")

    @OptIn(ExperimentalTime::class)
    suspend fun player(
        videoId: String,
        playlistId: String? = null,
        noLogIn: Boolean = false,
        shouldYtdlp: Boolean = false,
    ): Result<Triple<String?, PlayerResponse, MediaType>> =
        runCatching {
            val cpn =
                (1..16)
                    .map {
                        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_"[
                            Random.nextInt(
                                0,
                                64,
                            ),
                        ]
                    }.joinToString("")
            val listClients = listOf(WEB_REMIX, TVHTML5)

            var decodedSigResponse: PlayerResponse? = null
            var currentClient: YouTubeClient
            val now = Clock.System.now().epochSeconds
            if (now < poTokenObject.second) {
                Logger.d(TAG, "Use saved PoToken")
                poTokenObject.first
            } else {
                ytMusic
                    .createPoTokenChallenge()
                    .bodyAsText()
                    .let { challenge ->
                        val listChallenge = poTokenJsonDeserializer.decodeFromString<List<String?>>(challenge)
                        listChallenge.filterNotNull().firstOrNull()
                    }?.let { poTokenChallenge ->
                        ytMusic.generatePoToken(poTokenChallenge).bodyAsText().getPoToken().also { poToken ->
                            if (poToken != null) {
                                poTokenObject = Pair(poToken, now + 3600)
                            }
                        }
                    }
            }
            if (!noLogIn) {
                for (client in listClients) {
                    val tempRes =
                        ytMusic
                            .player(
                                WEB_REMIX,
                                videoId,
                                playlistId,
                                cpn,
                                signatureTimestamp =
                                    run {
                                        val today = Clock.System.todayIn(TimeZone.UTC)
                                        val epoch =
                                            Instant
                                                .fromEpochSeconds(0)
                                                .toLocalDateTime(TimeZone.UTC)
                                                .date
                                        epoch.daysUntil(today)
                                    },
                            ).body<PlayerResponse>()
                            .let {
                                val fexp =
                                    it.streamingData
                                        ?.serverAbrStreamingUrl
                                        ?.toKmpUri()
                                        ?.getQueryParameter("fexp")
                                val playbackTracking = it.playbackTracking
                                it.copy(
                                    playbackTracking =
                                        playbackTracking?.copy(
                                            atrUrl =
                                                playbackTracking.atrUrl?.copy(
                                                    baseUrl =
                                                        playbackTracking.atrUrl.baseUrl
                                                            ?.toKmpUri()
                                                            ?.buildUpon()
                                                            ?.apply {
                                                                if (fexp != null) {
                                                                    appendQueryParameter("fexp", fexp)
                                                                }
                                                            }?.build()
                                                            ?.toString(),
                                                ),
                                            videostatsPlaybackUrl =
                                                playbackTracking.videostatsPlaybackUrl?.copy(
                                                    baseUrl =
                                                        playbackTracking.videostatsPlaybackUrl.baseUrl
                                                            ?.toKmpUri()
                                                            ?.buildUpon()
                                                            ?.apply {
                                                                if (fexp != null) {
                                                                    appendQueryParameter("fexp", fexp)
                                                                }
                                                            }?.build()
                                                            ?.toString(),
                                                ),
                                            videostatsWatchtimeUrl =
                                                playbackTracking.videostatsWatchtimeUrl?.copy(
                                                    baseUrl =
                                                        playbackTracking.videostatsWatchtimeUrl.baseUrl
                                                            ?.toKmpUri()
                                                            ?.buildUpon()
                                                            ?.apply {
                                                                if (fexp != null) {
                                                                    appendQueryParameter("fexp", fexp)
                                                                }
                                                            }?.build()
                                                            ?.toString(),
                                                ),
                                        ),
                                )
                            }

                    val response =
                        if (noLogIn) {
                            var count = 0
                            var testUrl = ""
                            var res: PlayerResponse? = null
                            while (count < 3) {
                                val resp = newPipePlayer(videoId, tempRes)
                                testUrl = res
                                    ?.streamingData
                                    ?.adaptiveFormats
                                    ?.firstOrNull()
                                    ?.url ?: ""
                                val is403 = is403Url(testUrl)
                                if (!is403) {
                                    res = resp
                                    break
                                }
                                count++
                            }
                            res
                        } else if (shouldYtdlp) {
                            ytDlpPlayer(videoId, tempRes)
                        } else {
                            smartTubePlayer(videoId, tempRes) ?: newPipePlayer(videoId, tempRes)
                        }
                    if (response != null) {
                        decodedSigResponse = response
                        currentClient = client
                        Logger.d(TAG, "YouTube Player found URL with client ${currentClient.clientName}")
                        break
                    } else {
                        Logger.d(TAG, "YouTube Player no URL found with client ${client.clientName}")
                    }
                }
            } else {
                decodedSigResponse = null
            }
            if (decodedSigResponse == null) {
                val (tempCookie, visitorData, playbackTracking) = getVisitorData(videoId, playlistId)
                val poToken = poTokenObject.first
                Logger.d(TAG, "PoToken $poToken")
                val playerResponse = ytMusic.noLogInPlayer(videoId, tempCookie, visitorData, poToken ?: "").body<PlayerResponse>()
                Logger.d(TAG, "Player Response $playerResponse")
                Logger.d(TAG, "Thumbnails " + playerResponse.videoDetails?.thumbnail)
                Logger.d(TAG, "Player Response status: ${playerResponse.playabilityStatus.status}")
                val firstThumb =
                    playerResponse.videoDetails
                        ?.thumbnail
                        ?.thumbnails
                        ?.firstOrNull()
                val thumbnails =
                    if (firstThumb?.height == firstThumb?.width && firstThumb != null) MediaType.Song else MediaType.Video
                val formatList = playerResponse.streamingData?.formats?.map { Pair(it.itag, it.isAudio) }
                Logger.d(TAG, "Player Response formatList $formatList")
                val adaptiveFormatsList = playerResponse.streamingData?.adaptiveFormats?.map { Pair(it.itag, it.isAudio) }
                Logger.d(TAG, "Player Response adaptiveFormat $adaptiveFormatsList")
                val randomUrl = playerResponse.streamingData?.hlsManifestUrl
                playerResponse.streamingData
                    ?.formats
                    ?.randomOrNull()
                    ?.url
                    ?: playerResponse.streamingData
                        ?.adaptiveFormats
                        ?.randomOrNull()
                        ?.url
                Logger.d(TAG, "Player Response randomUrl $randomUrl")

                if (playerResponse.playabilityStatus.status == "OK" &&
                    (formatList != null || adaptiveFormatsList != null) &&
                    randomUrl != null &&
                    !is403Url(randomUrl)
                ) {
                    return@runCatching Triple(
                        cpn,
                        playerResponse.copy(
                            videoDetails = playerResponse.videoDetails?.copy(),
                            playbackTracking = playbackTracking ?: playerResponse.playbackTracking,
                        ),
                        thumbnails,
                    )
                } else {
                    Logger.d(TAG, "Player Response is not OK or formatList is null or randomUrl is null")
                    for (instance in listPipedInstances) {
                        try {
                            val piped = ytMusic.pipedStreams(videoId, instance).body<PipedResponse>()
                            val audioStreams = piped.audioStreams
                            val videoStreams = piped.videoStreams
                            val stream = audioStreams + videoStreams
                            return@runCatching Triple(
                                null,
                                playerResponse.copy(
                                    streamingData =
                                        PlayerResponse.StreamingData(
                                            formats = stream.toListFormat(),
                                            adaptiveFormats = stream.toListFormat(),
                                            expiresInSeconds = 0,
                                            hlsManifestUrl = piped.dash ?: piped.hls,
                                        ),
                                    videoDetails = playerResponse.videoDetails?.copy(),
                                    playbackTracking = playbackTracking ?: playerResponse.playbackTracking,
                                ),
                                thumbnails,
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            continue
                        }
                    }
                }
                throw Exception(playerResponse.playabilityStatus.status)
            } else {
                val firstThumb =
                    decodedSigResponse.videoDetails
                        ?.thumbnail
                        ?.thumbnails
                        ?.firstOrNull()
                val thumbnails =
                    if (firstThumb?.height == firstThumb?.width && firstThumb != null) MediaType.Song else MediaType.Video
                return@runCatching Triple(
                    cpn,
                    decodedSigResponse.copy(
                        videoDetails = decodedSigResponse.videoDetails?.copy(),
                        playbackTracking = decodedSigResponse.playbackTracking,
                    ),
                    thumbnails,
                )
            }
        }

    suspend fun updateWatchTime(
        watchtimeUrl: String,
        watchtimeList: ArrayList<Float>,
        cpn: String,
        playlistId: String?,
    ): Result<Int> =
        runCatching {
            val et = watchtimeList.takeLast(2).joinToString(",")
            val watchtime = watchtimeList.dropLast(1).takeLast(2).joinToString(",")
            ytMusic.initPlayback(watchtimeUrl, cpn, mapOf("st" to watchtime, "et" to et), playlistId).status.value.let { status ->
                if (status == 204) {
                    Logger.d(TAG, "watchtime done")
                }
                return@runCatching status
            }
        }

    suspend fun updateWatchTimeFull(
        watchtimeUrl: String,
        cpn: String,
        playlistId: String?,
    ): Result<Int> =
        runCatching {
            val regex = Regex("len=([^&]+)")
            val length =
                regex
                    .find(watchtimeUrl)
                    ?.groupValues
                    ?.firstOrNull()
                    ?.drop(4) ?: "0"
            Logger.d(TAG, length)
            ytMusic.initPlayback(watchtimeUrl, cpn, mapOf("st" to length, "et" to length), playlistId).status.value.let { status ->
                if (status == 204) {
                    Logger.d(TAG, "watchtime full done")
                }
                return@runCatching status
            }
        }

     
    suspend fun initPlayback(
        playbackUrl: String,
        atrUrl: String,
        watchtimeUrl: String,
        cpn: String,
        playlistId: String?,
    ): Result<Pair<Int, Float>> {
        Logger.d(TAG, "playbackUrl $playbackUrl")
        Logger.d(TAG, "atrUrl $atrUrl")
        Logger.d(TAG, "watchtimeUrl $watchtimeUrl")
        return runCatching {
            ytMusic.initPlayback(playbackUrl, cpn, null, playlistId).status.value.let { status ->
                if (status == 204) {
                    Logger.d(TAG, "playback done")
                    ytMusic.initPlayback(watchtimeUrl, cpn, mapOf("st" to "0", "et" to "5.54"), playlistId).status.value.let { firstWatchTime ->
                        if (firstWatchTime == 204) {
                            Logger.d(TAG, "first watchtime done")
                            delay(5000)
                            ytMusic.atr(atrUrl, cpn, null, playlistId).status.value.let { atr ->
                                if (atr == 204) {
                                    Logger.d(TAG, "atr done")
                                    delay(500)
                                    val secondWatchTime = (round(Random.nextFloat() * 100.0) / 100.0).toFloat() + 12f
                                    ytMusic
                                        .initPlayback(
                                            watchtimeUrl,
                                            cpn,
                                            mapOf<String, String>("st" to "0,5.54", "et" to "5.54,$secondWatchTime"),
                                            playlistId,
                                        ).status.value
                                        .let { watchtime ->
                                            if (watchtime == 204) {
                                                Logger.d(TAG, "watchtime done")
                                                return@runCatching Pair(watchtime, secondWatchTime)
                                            } else {
                                                return@runCatching Pair(watchtime, secondWatchTime)
                                            }
                                        }
                                } else {
                                    return@runCatching Pair(atr, 0f)
                                }
                            }
                        } else {
                            return@runCatching Pair(firstWatchTime, 0f)
                        }
                    }
                } else {
                    return@runCatching Pair(status, 0f)
                }
            }
        }
    }

    suspend fun nextYouTubePlaylists(continuation: String): Result<Pair<List<MusicTwoRowItemRenderer>, String?>> =
        runCatching {
            val res =
                ytMusic
                    .nextCtoken(
                        WEB_REMIX,
                        continuation,
                    )
            Logger.d(TAG, "Next Playlists ${res.bodyAsText()}")
            val response = res.body<BrowseResponse>()
            Pair(
                response
                    .continuationContents
                    ?.gridContinuation
                    ?.items
                    ?.mapNotNull { it.musicTwoRowItemRenderer } ?: emptyList(),
                response.continuationContents
                    ?.gridContinuation
                    ?.continuations
                    ?.getContinuation(),
            )
        }

    suspend fun next(
        endpoint: WatchEndpoint,
        continuation: String? = null,
    ): Result<NextResult> =
        runCatching {
            val response =
                ytMusic
                    .next(
                        WEB_REMIX,
                        endpoint.videoId,
                        endpoint.playlistId,
                        endpoint.playlistSetVideoId,
                        endpoint.index,
                        endpoint.params,
                        continuation,
                    ).body<NextResponse>()
            val playlistPanelRenderer =
                response.continuationContents?.playlistPanelContinuation
                    ?: response.contents.singleColumnMusicWatchNextResultsRenderer
                        ?.tabbedRenderer
                        ?.watchNextTabbedResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.musicQueueRenderer
                        ?.content
                        ?.playlistPanelRenderer
            if (playlistPanelRenderer != null) {
                 
                if (playlistPanelRenderer.contents
                        .lastOrNull()
                        ?.automixPreviewVideoRenderer
                        ?.content
                        ?.automixPlaylistVideoRenderer
                        ?.navigationEndpoint
                        ?.watchPlaylistEndpoint !=
                    null
                ) {
                    return@runCatching next(
                        playlistPanelRenderer.contents
                            .lastOrNull()
                            ?.automixPreviewVideoRenderer
                            ?.content
                            ?.automixPlaylistVideoRenderer
                            ?.navigationEndpoint
                            ?.watchPlaylistEndpoint!!,
                    ).getOrThrow()
                        .let { result ->
                            result.copy(
                                title = playlistPanelRenderer.title,
                                items =
                                    playlistPanelRenderer.contents.mapNotNull {
                                        it.playlistPanelVideoRenderer?.let { renderer ->
                                            NextPage.fromPlaylistPanelVideoRenderer(renderer)
                                        }
                                    } + result.items,
                                lyricsEndpoint =
                                    response.contents.singleColumnMusicWatchNextResultsRenderer
                                        ?.tabbedRenderer
                                        ?.watchNextTabbedResultsRenderer
                                        ?.tabs
                                        ?.getOrNull(
                                            1,
                                        )?.tabRenderer
                                        ?.endpoint
                                        ?.browseEndpoint,
                                relatedEndpoint =
                                    response.contents.singleColumnMusicWatchNextResultsRenderer
                                        ?.tabbedRenderer
                                        ?.watchNextTabbedResultsRenderer
                                        ?.tabs
                                        ?.getOrNull(
                                            2,
                                        )?.tabRenderer
                                        ?.endpoint
                                        ?.browseEndpoint,
                                currentIndex = playlistPanelRenderer.currentIndex,
                                endpoint =
                                    playlistPanelRenderer.contents
                                        .lastOrNull()
                                        ?.automixPreviewVideoRenderer
                                        ?.content
                                        ?.automixPlaylistVideoRenderer
                                        ?.navigationEndpoint
                                        ?.watchPlaylistEndpoint!!,
                            )
                        }
                }
 
 
 
                return@runCatching NextResult(
                    title = playlistPanelRenderer.title,
                    items =
                        playlistPanelRenderer.contents.mapNotNull {
                            it.playlistPanelVideoRenderer?.let(NextPage::fromPlaylistPanelVideoRenderer)
                        },
                    currentIndex = playlistPanelRenderer.currentIndex,
                    lyricsEndpoint =
                        response.contents.singleColumnMusicWatchNextResultsRenderer
                            ?.tabbedRenderer
                            ?.watchNextTabbedResultsRenderer
                            ?.tabs
                            ?.getOrNull(
                                1,
                            )?.tabRenderer
                            ?.endpoint
                            ?.browseEndpoint,
                    relatedEndpoint =
                        response.contents.singleColumnMusicWatchNextResultsRenderer
                            ?.tabbedRenderer
                            ?.watchNextTabbedResultsRenderer
                            ?.tabs
                            ?.getOrNull(
                                2,
                            )?.tabRenderer
                            ?.endpoint
                            ?.browseEndpoint,
                    continuation = playlistPanelRenderer.continuations?.getContinuation(),
                    endpoint = endpoint,
                )
            } else {
                val musicPlaylistShelfContinuation = response.continuationContents?.musicPlaylistShelfContinuation!!
                return@runCatching NextResult(
                    items =
                        musicPlaylistShelfContinuation.contents.mapNotNull {
                            it.musicResponsiveListItemRenderer?.let { renderer ->
                                NextPage.fromMusicResponsiveListItemRenderer(renderer)
                            }
                        },
                    continuation =
                        musicPlaylistShelfContinuation.continuations
                            ?.firstOrNull()
                            ?.nextContinuationData
                            ?.continuation,
                    endpoint =
                        WatchEndpoint(
                            videoId = null,
                            playlistId = null,
                            playlistSetVideoId = null,
                            params = null,
                            index = null,
                            watchEndpointMusicSupportedConfigs = null,
                        ),
                )
            }
        }

    suspend fun lyrics(endpoint: BrowseEndpoint): Result<String?> =
        runCatching {
            val response = ytMusic.browse(WEB_REMIX, endpoint.browseId, endpoint.params).body<BrowseResponse>()
            response.contents
                ?.sectionListRenderer
                ?.contents
                ?.firstOrNull()
                ?.musicDescriptionShelfRenderer
                ?.description
                ?.runs
                ?.firstOrNull()
                ?.text
        }

    suspend fun queue(
        videoIds: List<String>? = null,
        playlistId: String? = null,
    ): Result<List<SongItem>> =
        runCatching {
            ytMusic
                .getQueue(WEB_REMIX, videoIds, playlistId)
                .body<GetQueueResponse>()
                .queueDatas
                .mapNotNull {
                    it.content.playlistPanelVideoRenderer?.let { renderer ->
                        NextPage.fromPlaylistPanelVideoRenderer(renderer)
                    }
                }
        }

    suspend fun visitorData(): String? =
        try {
            Json
                .parseToJsonElement(ytMusic.getSwJsData().bodyAsText().substring(5))
                .jsonArray[0]
                .jsonArray[2]
                .jsonArray
                .first { (it as? JsonPrimitive)?.content?.startsWith(VISITOR_DATA_PREFIX) == true }
                .jsonPrimitive.content
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

    suspend fun accountInfo(customCookie: String? = null): Result<AccountInfo?> =
        runCatching {
            ytMusic
                .accountMenu(customCookie, WEB_REMIX)
                .apply {
                    Logger.d(TAG, this.bodyAsText())
                }.body<AccountMenuResponse>()
                .actions[0]
                .openPopupAction.popup.multiPageMenuRenderer.header
                ?.activeAccountHeaderRenderer
                ?.toAccountInfo()
        }

    suspend fun getAccountListWithPageId(customCookie: String): Result<List<AccountInfo>> =
        runCatching {
            val res =
                ytMusic
                    .getAccountSwitcherEndpoint(customCookie)
                    .bodyAsText()
                    .removePrefix(")]}'\n")
            val accountSwitcherEndpointResponse = ytMusic.normalJson.decodeFromString<AccountSwitcherEndpointResponse>(res)
            Logger.d(TAG, "Account List Response: $accountSwitcherEndpointResponse")
            accountSwitcherEndpointResponse.toListAccountInfo()
        }

    suspend fun pipeStream(
        videoId: String,
        pipedInstance: String,
    ) = runCatching {
        ytMusic.pipedStreams(videoId, pipedInstance).body<PipedResponse>()
    }

    suspend fun getLibraryPlaylists() =
        runCatching {
            ytMusic.browse(WEB_REMIX, "FEmusic_liked_playlists", setLogin = true).body<BrowseResponse>()
        }

    suspend fun getMixedForYou() =
        runCatching {
            ytMusic.browse(WEB_REMIX, "FEmusic_mixed_for_you", setLogin = true).body<BrowseResponse>()
        }

    @JvmInline
    value class SearchFilter(
        val value: String,
    ) {
        companion object {
            val FILTER_SONG = SearchFilter("EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D")
            val FILTER_VIDEO = SearchFilter("EgWKAQIQAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_ALBUM = SearchFilter("EgWKAQIYAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_ARTIST = SearchFilter("EgWKAQIgAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_FEATURED_PLAYLIST = SearchFilter("EgeKAQQoADgBagwQDhAKEAMQBRAJEAQ%3D")
            val FILTER_COMMUNITY_PLAYLIST = SearchFilter("EgeKAQQoAEABagoQAxAEEAoQCRAF")
            val FILTER_PODCAST = SearchFilter("EgWKAQJQAWoIEBAQERADEBU%3D")
        }
    }

    suspend fun getYTMusicSearchSuggestions(query: String) =
        runCatching {
            val response = ytMusic.getSearchSuggestions(WEB_REMIX, query).body<GetSearchSuggestionsResponse>()
            SearchSuggestions(
                queries =
                    response.contents
                        ?.getOrNull(0)
                        ?.searchSuggestionsSectionRenderer
                        ?.contents
                        ?.mapNotNull { content ->
                            content.searchSuggestionRenderer
                                ?.suggestion
                                ?.runs
                                ?.joinToString(separator = "") { it.text }
                        }.orEmpty(),
                recommendedItems =
                    response.contents
                        ?.getOrNull(1)
                        ?.searchSuggestionsSectionRenderer
                        ?.contents
                        ?.mapNotNull {
                            it.musicResponsiveListItemRenderer?.let { renderer ->
                                SearchSuggestionPage.fromMusicResponsiveListItemRenderer(renderer)
                            }
                        }.orEmpty(),
            )
        }

    suspend fun getYouTubeCaption(
        videoId: String,
        preferLang: String,
    ) = runCatching {
        val ytWeb = ytMusic.player(WEB, videoId, null, null).body<YouTubeInitialPage>()
        val baseCaption =
            ytMusic
                .getYouTubeCaption(
                    ytWeb.captions?.playerCaptionsTracklistRenderer?.captionTracks?.firstOrNull()?.baseUrl?.replace(
                        "&fmt=srv3",
                        "",
                    ) ?: "",
                ).body<Transcript>()
                .tryDecodeText()
        val translateCaption =
            try {
                ytMusic
                    .getYouTubeCaption(
                        "${
                            ytWeb.captions?.playerCaptionsTracklistRenderer?.captionTracks?.firstOrNull()?.baseUrl?.replace(
                                "&fmt=srv3",
                                "",
                            )
                        }&tlang=$preferLang",
                    ).body<Transcript>()
                    .tryDecodeText()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        return@runCatching baseCaption to translateCaption
    }

    suspend fun scrapeYouTube(videoId: String) =
        runCatching {
            ytMusic.scrapeYouTube(videoId).body<String>()
        }

    suspend fun removeItemYouTubePlaylist(
        playlistId: String,
        videoId: String,
        setVideoId: String,
    ) = runCatching {
        ytMusic.removeItemYouTubePlaylist(playlistId, videoId, setVideoId).status.value
    }

    suspend fun addPlaylistItem(
        playlistId: String,
        videoId: String,
    ) = runCatching {
        ytMusic.addItemYouTubePlaylist(playlistId, videoId).body<AddItemYouTubePlaylistResponse>()
    }

    suspend fun editPlaylist(
        playlistId: String,
        title: String,
    ) = runCatching {
        ytMusic.editYouTubePlaylist(playlistId, title).status.value
    }

    suspend fun createPlaylist(
        title: String,
        listVideoId: List<String>?,
    ) = runCatching {
        ytMusic.createYouTubePlaylist(title, listVideoId).body<CreatePlaylistResponse>()
    }

    suspend fun addToLiked(mediaId: String) =
        runCatching {
            ytMusic.addToLiked(mediaId).status.value
        }

    suspend fun removeFromLiked(mediaId: String) =
        runCatching {
            ytMusic.removeFromLiked(mediaId).status.value
        }

    private fun getNParam(listFormat: List<PlayerResponse.StreamingData.Format>): String? =
        listFormat
            .firstOrNull { it.itag == 251 }
            ?.let {
                val sc = it.signatureCipher ?: it.url ?: return null
                val params = parseQueryString(sc)
                val url =
                    params["url"]?.let { URLBuilder(it) }
                        ?: run {
                            Logger.e(TAG, "Could not parse cipher url")
                            return null
                        }
                url.parameters["n"]
            }

    fun download(
        track: SongItem,
        filePath: String,
        videoId: String,
        isVideo: Boolean = false,
        shouldYtdlp: Boolean,
    ): Flow<DownloadProgress> =
        channelFlow {
             
            trySend(DownloadProgress(0.01f))
            player(videoId = videoId, shouldYtdlp = shouldYtdlp, noLogIn = false)
                .onSuccess { playerResponse ->
                    val audioFormat =
                        listOf(
                            playerResponse.second.streamingData
                                ?.formats
                                ?.filter { it.isAudio && it.url != null }
                                ?.maxByOrNull { it.bitrate },
                            playerResponse.second.streamingData
                                ?.adaptiveFormats
                                ?.filter { it.isAudio && it.url != null }
                                ?.maxByOrNull { it.bitrate },
                        ).maxByOrNull { it?.bitrate ?: 0 }
                    val videoFormat =
                        listOf(
                            playerResponse.second.streamingData
                                ?.formats
                                ?.filter { !it.isAudio && it.url != null }
                                ?.maxByOrNull { it.bitrate },
                            playerResponse.second.streamingData
                                ?.adaptiveFormats
                                ?.filter { !it.isAudio && it.url != null }
                                ?.maxByOrNull { it.bitrate },
                        ).maxByOrNull { it?.bitrate ?: 0 }
                    Logger.d(TAG, "Audio Format $audioFormat")
                    Logger.d(TAG, "Video Format $videoFormat")
                    val audioUrl =
                        audioFormat?.url ?: run {
                            trySend(DownloadProgress.failed("Audio format url is null"))
                            return@channelFlow
                        }
                    if (isVideo) {
                        runCatching {
                            val videoUrl = videoFormat?.url ?: throw Exception("Video format url is null")
                            val downloadAudioJob = ytMusic.download(audioUrl, ("$filePath.webm"))
                            val downloadVideoJob = ytMusic.download(videoUrl, ("$filePath.mp4"))
                            combine(downloadVideoJob, downloadAudioJob) { videoProgress, audioProgress ->
                                Pair(videoProgress, audioProgress)
                            }.collectLatest { (videoProgress, audioProgress) ->
                                if (!videoProgress.first || !audioProgress.first) {
                                    trySend(
                                        DownloadProgress(
                                            videoDownloadProgress = videoProgress.second,
                                            audioDownloadProgress = audioProgress.second,
                                            downloadSpeed = if (videoProgress.third != 0) videoProgress.third else audioProgress.third,
                                        ),
                                    )
                                } else {
                                    trySend(DownloadProgress.MERGING)
                                    trySend(
                                        ytMusic.mergeAudioVideoDownload(
                                            filePath,
                                        ),
                                    )
                                }
                            }
                        }.onSuccess {
                            Logger.d(TAG, "Download Video Success")
                        }.onFailure {
                            it.printStackTrace()
                            trySend(DownloadProgress.failed(it.message ?: "Download failed"))
                        }
                    } else {
                         
                        runCatching {
                            ytMusic
                                .download(audioUrl, ("$filePath.webm"))
                                .collect { downloadProgress ->
                                    if (!downloadProgress.first) {
                                        trySend(DownloadProgress(audioDownloadProgress = downloadProgress.second))
                                    } else {
                                        trySend(DownloadProgress(audioDownloadProgress = 1f, isDone = true))
                                    }
                                }
                        }.onSuccess {
                            Logger.d(TAG, "Download only Audio Success")
                             
                            trySend(
                                ytMusic.saveAudioWithThumbnail(
                                    filePath,
                                    track,
                                ),
                            )
                        }.onFailure { e ->
                            e.printStackTrace()
                            trySend(DownloadProgress.failed(e.message ?: "Download failed"))
                        }
                    }
                }.onFailure {
                    it.printStackTrace()
                    Logger.d(TAG, "Player Response is null")
                    trySend(DownloadProgress.failed(it.message ?: "Player response is null"))
                }
        }.flowOn(Dispatchers.IO)

    suspend fun is403Url(url: String) = ytMusic.is403Url(url)

    companion object {
        const val MAX_GET_QUEUE_SIZE = 1000

        private const val VISITOR_DATA_PREFIX = "Cgt"

        const val DEFAULT_VISITOR_DATA = "CgtsZG1ySnZiQWtSbyiMjuGSBg%3D%3D"
    }
}

