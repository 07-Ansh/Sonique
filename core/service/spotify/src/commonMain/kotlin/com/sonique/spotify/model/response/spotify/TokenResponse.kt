package com.sonique.spotify.model.response.spotify

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
@ExperimentalSerializationApi
data class TokenResponse(
    @ProtoNumber(1)
    @SerialName("response_type")
    val responseType: String?,
    @ProtoNumber(2)
    @SerialName("granted_token")
    val grantedToken: GrantedToken?,
) {
    @Serializable
    data class GrantedToken(
        @ProtoNumber(1)
        val token: String?,
        @ProtoNumber(2)
        @SerialName("expires_after_seconds")
        val expiresAfterSeconds: Int?,
        @ProtoNumber(3)
        @SerialName("refresh_after_seconds")
        val refreshAfterSeconds: Int?,
        @ProtoNumber(4)
        val domains: List<Domain>?,
    ) {
        @Serializable
        data class Domain(
            @ProtoNumber(1)
            val domain: String?,
        )
    }
}
 

