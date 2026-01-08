package com.liskovsoft.googleapi.common.helpers

object DefaultHeaders {
    private const val USER_AGENT_FIRE_TV =
        "Mozilla/5.0 (Linux armeabi-v7a; Android 7.1.2; Fire OS 6.0) Cobalt/22.lts.3.306369-gold (unlike Gecko) v8/8.8.278.8-jit gles Starboard/13, Amazon_ATV_mediatek8695_2019/NS6294 (Amazon, AFTMM, Wireless) com.amazon.firetv.youtube/22.3.r2.v66.0"

    @JvmField
    val APP_USER_AGENT = USER_AGENT_FIRE_TV

    const val ACCEPT_ENCODING = "gzip, deflate, br"
}
