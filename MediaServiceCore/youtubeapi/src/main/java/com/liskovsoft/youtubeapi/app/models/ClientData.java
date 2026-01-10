package com.liskovsoft.youtubeapi.app.models;

import com.liskovsoft.googlecommon.common.converters.regexp.RegExp;

 
public class ClientData {
     
    @RegExp({
            "clientId:\"([-\\w]+\\.apps\\.googleusercontent\\.com)\",\\n?[$\\w]+:\"\\w+\"",
             
             
    })
    private String mClientId;

     
    @RegExp({
            "clientId:\"[-\\w]+\\.apps\\.googleusercontent\\.com\",\\n?[$\\w]+:\"(\\w+)\"",
             
             
    })
    private String mClientSecret;

    public String getClientId() {
        return mClientId;
    }
    
    public String getClientSecret() {
        return mClientSecret;
    }
}
