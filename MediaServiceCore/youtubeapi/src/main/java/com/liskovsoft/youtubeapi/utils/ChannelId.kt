package com.liskovsoft.youtubeapi.utils

import com.liskovsoft.googlecommon.common.converters.regexp.RegExp

internal class ChannelId {
     
    @RegExp("<link rel=\"canonical\" href=\"https://www.youtube.com/channel/([\\w-]+)\">")
    var channelId: String? = null
        private set
}


