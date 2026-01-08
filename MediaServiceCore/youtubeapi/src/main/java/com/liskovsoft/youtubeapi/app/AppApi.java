package com.liskovsoft.youtubeapi.app;

import com.liskovsoft.youtubeapi.app.models.AppInfo;
import com.liskovsoft.youtubeapi.app.models.ClientData;
import com.liskovsoft.youtubeapi.app.models.PlayerData;
import com.liskovsoft.googlecommon.common.converters.regexp.WithRegExp;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Url;

@WithRegExp
public interface AppApi {
     
    @GET("https://www.youtube.com/tv")
    Call<AppInfo> getAppInfo(@Header("User-Agent") String userAgent);

     
    @GET("https://www.youtube.com/tv")
    Call<AppInfo> getAppInfo(@Header("User-Agent") String userAgent, @Header("Cookie") String visitorInfoLive);

     
    @GET
    Call<PlayerData> getPlayerData(@Url String playerUrl);

     
    @GET
    Call<ClientData> getClientData(@Url String clientUrl);
}
