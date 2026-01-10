package com.sonique.domain.data.type

 
interface HomeContentType

 
interface LibraryType

 
interface PlaylistType :
    HomeContentType,
    LibraryType {
    enum class Type {
        YOUTUBE_PLAYLIST,
        RADIO,
        LOCAL,
        ALBUM,
        PODCAST,
    }

    fun playlistType(): Type
}

interface ArtistType

interface RecentlyType : LibraryType {
    enum class Type {
        SONG,
        ALBUM,
        ARTIST,
        PLAYLIST,
    }

    fun objectType(): Type
}

 
interface SearchResultType {
    enum class Type {
        SONG,
        VIDEO,
        PLAYLIST,
        ALBUM,
        ARTIST,
        PODCAST,
    }

    fun objectType(): Type
}

