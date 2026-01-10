package com.liskovsoft.googleapi.oauth2.manager;

import com.liskovsoft.googlecommon.common.helpers.RetrofitOkHttpHelper;
import com.liskovsoft.googlecommon.common.models.auth.AccessToken;
import com.liskovsoft.googlecommon.service.oauth.YouTubeAccount;
import com.liskovsoft.mediaserviceinterfaces.oauth.Account;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.mylogger.Log;

import java.util.Map;

public abstract class OAuth2AccountManagerBase {
    private static final String TAG = OAuth2AccountManagerBase.class.getSimpleName();
    private static final long TOKEN_REFRESH_PERIOD_MS = 60 * 60 * 1_000;  
    private String mCachedAuthorizationHeader;
    private long mCacheUpdateTime;

    protected abstract Account getSelectedAccount();
    protected abstract AccessToken obtainAccessToken(String refreshToken);

    public void checkAuth() {
        updateAuthHeadersIfNeeded();
    }

    protected void updateAuthHeadersIfNeeded() {
        if (mCachedAuthorizationHeader != null && Helpers.equals(mCachedAuthorizationHeader, RetrofitOkHttpHelper.getAuthHeaders().get("Authorization"))
                && System.currentTimeMillis() - mCacheUpdateTime < TOKEN_REFRESH_PERIOD_MS) {
            return;
        }

        updateAuthHeaders();

        mCacheUpdateTime = System.currentTimeMillis();
    }

    private void updateAuthHeaders() {
        Account account = getSelectedAccount();
        String refreshToken = account != null ? ((YouTubeAccount) account).getRefreshToken() : null;
         
        mCachedAuthorizationHeader = createAuthorizationHeader(refreshToken);
        syncWithRetrofit();
    }

     
    public void setAuthorizationHeader(String authorizationHeader) {
        mCachedAuthorizationHeader = authorizationHeader;
        mCacheUpdateTime = System.currentTimeMillis();

        syncWithRetrofit();
    }

    public void invalidateCache() {
        mCachedAuthorizationHeader = null;
    }

     
    private synchronized String createAuthorizationHeader(String refreshToken) {
        Log.d(TAG, "Updating authorization header...");

        String authorizationHeader = null;

        AccessToken token = obtainAccessToken(refreshToken);

        if (token != null) {
            authorizationHeader = String.format("%s %s", token.getTokenType(), token.getAccessToken());
        } else {
            Log.e(TAG, "Access token is null!");
        }

        return authorizationHeader;
    }

    private void syncWithRetrofit() {
        if (Helpers.isJUnitTest()) {
            return;
        }

        Map<String, String> headers = RetrofitOkHttpHelper.getAuthHeaders();
        headers.clear();

        if (mCachedAuthorizationHeader != null && getSelectedAccount() != null) {
            headers.put("Authorization", mCachedAuthorizationHeader);
            String pageIdToken = ((YouTubeAccount) getSelectedAccount()).getPageIdToken();
            if (pageIdToken != null) {
                 
                headers.put("X-Goog-Pageid", pageIdToken);
            }
        }
    }
}
