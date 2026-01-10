package com.sonique.domain.data.player

 
data class PlayerError(
    val errorCode: Int,
    val errorCodeName: String,
    val message: String?,
)

