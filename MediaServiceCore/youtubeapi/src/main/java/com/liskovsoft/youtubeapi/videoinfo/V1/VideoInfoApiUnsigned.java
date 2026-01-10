package com.liskovsoft.youtubeapi.videoinfo.V1;

import com.liskovsoft.youtubeapi.common.helpers.AppConstants;
import com.liskovsoft.googlecommon.common.converters.querystring.WithQueryString;
import com.liskovsoft.youtubeapi.videoinfo.models.VideoInfo;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

 
@WithQueryString
public interface VideoInfoApiUnsigned {
     
    @GET(AppConstants.GET_VIDEO_INFO_OLD)
    Call<VideoInfo> getVideoInfo(@Query("video_id") String videoId, @Query("hl") String lang, @Query("sts") String sts, @Query("cver") String clientVersion);
    
    @GET(AppConstants.GET_VIDEO_INFO_OLD)
    Call<VideoInfo> getVideoInfo(@Query("video_id") String videoId, @Query("hl") String lang, @Query("cver") String clientVersion);

     
    @GET(AppConstants.GET_VIDEO_INFO_OLD)
    Call<VideoInfo> getVideoInfoHls(@Query("video_id") String videoId, @Query("hl") String lang, @Query("cver") String clientVersion);

     
    @GET(AppConstants.GET_VIDEO_INFO_OLD2)
    Call<VideoInfo> getVideoInfoRestricted(@Query("video_id") String videoId, @Query("hl") String lang, @Query("cver") String clientVersion);
}
