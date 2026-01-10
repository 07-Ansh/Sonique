package com.liskovsoft.googleapi.oauth2;

import com.liskovsoft.googlecommon.common.helpers.RetrofitHelper;
import com.liskovsoft.googlecommon.common.models.auth.AccessToken;
import com.liskovsoft.googlecommon.common.models.auth.UserCode;
import com.liskovsoft.sharedutils.mylogger.Log;

import retrofit2.Call;

public class OAuth2Service {
    private static final String TAG = OAuth2Service.class.getSimpleName();
    private static OAuth2Service sInstance;
    private final OAuth2Api mOAuth2Api;
    private static final int REFRESH_TOKEN_ATTEMPTS = 200;
    private static final long REFRESH_TOKEN_ATTEMPT_INTERVAL_MS = 5_000;

    private OAuth2Service() {
        mOAuth2Api = RetrofitHelper.create(OAuth2Api.class);
    }

    public static OAuth2Service instance() {
        if (sInstance == null) {
            sInstance = new OAuth2Service();
        }

        return sInstance;
    }

     
    public UserCode getUserCode() {
        Call<UserCode> wrapper = mOAuth2Api.getUserCode(OAuth2ApiHelper.CLIENT_ID, OAuth2ApiHelper.DRIVE_SCOPE);
        return RetrofitHelper.get(wrapper);
    }

     
    private AccessToken getAccessToken(String deviceCode) {
        Call<AccessToken> wrapper = mOAuth2Api.getAccessToken(OAuth2ApiHelper.CLIENT_ID, OAuth2ApiHelper.CLIENT_SECRET, deviceCode, OAuth2ApiHelper.GRANT_TYPE);
        return RetrofitHelper.get(wrapper);
    }

     
    public AccessToken updateAccessToken(String refreshToken) {
        Call<AccessToken> wrapper = mOAuth2Api.updateAccessToken(OAuth2ApiHelper.CLIENT_ID, OAuth2ApiHelper.CLIENT_SECRET, OAuth2ApiHelper.GRANT_TYPE_REFRESH, refreshToken);
        return RetrofitHelper.getWithErrors(wrapper);
    }

    public AccessToken getAccessTokenWait(String deviceCode) throws InterruptedException {
        AccessToken tokenResult = null;

        for (int i = 0; i < REFRESH_TOKEN_ATTEMPTS; i++) {
            Thread.sleep(REFRESH_TOKEN_ATTEMPT_INTERVAL_MS);

            tokenResult = getAccessToken(deviceCode);

            if (tokenResult != null && tokenResult.getRefreshToken() != null) {
                break;
            }
        }

        if (tokenResult != null && tokenResult.getRefreshToken() != null) {
            return tokenResult;
        } else {
            String msg = String.format("Error. Refresh token is empty!\nDebug data: device code: %s, client id: %s, client secret: %s\nError msg: %s",
                    deviceCode,
                    null,  
                    null,  
                    tokenResult != null ? tokenResult.getError() : "");

            Log.e(TAG, msg);
            throw new IllegalStateException(msg);
        }
    }

     
     
     
     
     
     
     
}
