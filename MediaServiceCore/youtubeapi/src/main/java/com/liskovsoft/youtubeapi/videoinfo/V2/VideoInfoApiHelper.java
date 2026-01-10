package com.liskovsoft.youtubeapi.videoinfo.V2;

import com.liskovsoft.youtubeapi.app.PoTokenGate;
import com.liskovsoft.youtubeapi.common.helpers.AppClient;
import com.liskovsoft.youtubeapi.common.helpers.QueryBuilder;

public class VideoInfoApiHelper {
    public static String getVideoInfoQuery(AppClient client, String videoId, String clickTrackingParams) {
        return createCheckedQuery(client, videoId, clickTrackingParams, client == AppClient.GEO);
    }

     
     
     
     
     
     

     
    private static String createCheckedQuery(AppClient client, String videoId, String clickTrackingParams, boolean enableGeoFix) {
         
         
        return new QueryBuilder(client)
                .setVideoId(videoId)
                .setClickTrackingParams(clickTrackingParams)
                .setPoToken(PoTokenGate.getPoToken(client, videoId))
                .setVisitorData(PoTokenGate.getVisitorData(client))
                .enableGeoFix(enableGeoFix)  
                .build();
    }
}
