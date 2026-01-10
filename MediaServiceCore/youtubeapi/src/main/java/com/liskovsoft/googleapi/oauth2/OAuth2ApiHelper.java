package com.liskovsoft.googleapi.oauth2;

import com.liskovsoft.googlecommon.common.ApiKeys;

 
class OAuth2ApiHelper {
    public static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:device_code";
    public static final String GRANT_TYPE_REFRESH = "refresh_token";

    public static final String CLIENT_ID = ApiKeys.CLIENT_ID;
    public static final String CLIENT_SECRET = ApiKeys.CLIENT_SECRET;

     
    public static final String DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file";
    public static final String YOUTUBE_SCOPE = "https://www.googleapis.com/auth/youtube https://www.googleapis.com/auth/youtube.readonly";
    public static final String SIGN_IN_DATA_SCOPE = "email openid profile";
}
