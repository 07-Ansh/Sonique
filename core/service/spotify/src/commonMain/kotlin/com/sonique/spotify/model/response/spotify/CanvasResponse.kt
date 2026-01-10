package com.sonique.spotify.model.response.spotify

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
data class CanvasResponse(
    @ProtoNumber(1)
    val canvases: List<Canvas>,
) {
    @Serializable
    data class Canvas(
        @ProtoNumber(1)
        val id: String,
        @ProtoNumber(2)
        val canvas_url: String,
        @ProtoNumber(5)
        val track_uri: String,
        @ProtoNumber(6)
        val artist: Artist,
        @ProtoNumber(9)
        val other_id: String? = null,
        @ProtoNumber(11)
        val canvas_uri: String,
        @ProtoNumber(13)
        val thumbsOfCanva: List<ThumbOfCanva>? = null,
    ) {
        @Serializable
        data class Artist(
            @ProtoNumber(1)
            val artist_uri: String,
            @ProtoNumber(2)
            val artist_name: String,
            @ProtoNumber(3)
            val artist_img_url: String,
        )

        @Serializable
        data class ThumbOfCanva(
            @ProtoNumber(1)
            val height: Int? = null,
            @ProtoNumber(2)
            val width: Int? = null,
            @ProtoNumber(3)
            val url: String? = null,
        )
    }
}

 
 
 
 
 
 
 
 
 
 
 
 
 
 
 
 

