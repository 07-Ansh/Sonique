package com.liskovsoft.youtubeapi.app;

import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.youtubeapi.app.models.AppInfo;
import com.liskovsoft.youtubeapi.app.models.ClientData;
import com.liskovsoft.youtubeapi.app.playerdata.PlayerDataExtractor;
import com.liskovsoft.googlecommon.common.helpers.DefaultHeaders;
import com.liskovsoft.googlecommon.common.helpers.RetrofitHelper;
import com.liskovsoft.youtubeapi.service.internal.MediaServiceData;

import retrofit2.Call;
import retrofit2.Response;

public class AppServiceInt {
    private static final String TAG = AppServiceInt.class.getSimpleName();
    private final AppApi mAppApi;

    public AppServiceInt() {
        mAppApi = RetrofitHelper.create(AppApi.class);
    }

     
    protected AppInfo getAppInfo(String userAgent) {
        String visitorCookie = getData().getVisitorCookie();
        Call<AppInfo> wrapper = mAppApi.getAppInfo(userAgent, visitorCookie);
        AppInfo result = null;

        Response<AppInfo> response = RetrofitHelper.getResponse(wrapper);

        if (response != null) {
             
             
             
            getData().setVisitorCookie(RetrofitHelper.getCookies(response));
            result = response.body();
        }

        return result;
    }

    public PlayerDataExtractor getPlayerDataExtractor(String playerUrl) {
        return new PlayerDataExtractor(playerUrl);
    }

    protected ClientData getClientData(String clientUrl) {
        if (clientUrl == null) {
            return null;
        }

        Call<ClientData> wrapper = mAppApi.getClientData(clientUrl);
        ClientData clientData = RetrofitHelper.get(wrapper);

         
        if (clientData == null) {
            clientData = RetrofitHelper.get(mAppApi.getClientData(getLegacyClientUrl(clientUrl)));
        }

        return clientData;
    }
    
    private static String getLegacyClientUrl(String clientUrl) {
        if (clientUrl == null) {
            return null;
        }

        return clientUrl
                .replace("/dg=0/", "/exm=base/ed=1/")
                .replace("/m=base", "/m=main");
    }

    public void invalidateVisitorData() {
        getData().setVisitorCookie(null);
    }

    public void invalidateCache() {
         
    }

    public boolean isPlayerCacheActual() {
         
        return false;
    }

     

    public String getClientId() {
         
        ClientData clientData = getClientData();
        return clientData != null ? clientData.getClientId() : null;
    }

     
    public String getClientSecret() {
        return getClientData() != null ? getClientData().getClientSecret() : null;
    }

     
    public String getVisitorData() {
         
        return getAppInfoData() != null ? getAppInfoData().getVisitorData() : null;
    }

    public String getPlayerUrl() {
         
         
        return getAppInfoData() != null ? getAppInfoData().getPlayerUrl() : null;
    }

    public String getClientUrl() {
         
        return getAppInfoData() != null ? getAppInfoData().getClientUrl() : null;
    }

    private AppInfo getAppInfoData() {
        return getAppInfo(DefaultHeaders.APP_USER_AGENT);
    }

    private ClientData getClientData() {
        return getClientData(getClientUrl());
    }

    public PlayerDataExtractor getPlayerDataExtractor() {
        return getPlayerDataExtractor(getPlayerUrl());
    }

    public void refreshCacheIfNeeded() {
        getAppInfoData();
        getClientData();
        getPlayerDataExtractor();
    }

    protected MediaServiceData getData() {
        return MediaServiceData.instance();
    }
}
