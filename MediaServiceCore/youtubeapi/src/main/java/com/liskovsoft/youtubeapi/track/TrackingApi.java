package com.liskovsoft.youtubeapi.track;

import com.liskovsoft.googlecommon.common.converters.jsonpath.WithJsonPath;
import com.liskovsoft.youtubeapi.track.models.WatchTimeEmptyResult;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

@WithJsonPath
public interface TrackingApi {
     
    @GET("https://www.youtube.com/api/stats/playback?ns=yt&ver=2")
    Call<WatchTimeEmptyResult> createWatchRecord(
            @Query("docid") String videoId,               
            @Query("len") float lengthSec,                
            @Query("cmt") float watchStartSec,            
            @Query("cpn") String clientPlaybackNonce,     
            @Query("ei") String eventId,                  
            @Query("vm") String vm,                       
            @Query("of") String of                        
    );

     
    @GET("https://www.youtube.com/api/stats/playback?ns=yt&ver=2&final=1")
    Call<WatchTimeEmptyResult> createWatchRecordShort(
            @Query("docid") String videoId,               
            @Query("cpn") String clientPlaybackNonce,     
            @Query("ei") String eventId,                  
            @Query("vm") String vm,                       
            @Query("of") String of                        
    );

     
    @GET("https://www.youtube.com/api/stats/watchtime?ns=yt&ver=2")
    Call<WatchTimeEmptyResult> updateWatchTime(
            @Query("docid") String videoId,               
            @Query("len") float lengthSec,                
            @Query("st") float jumpFromToSec,             
            @Query("et") float jumpFromToSecAlt,          
            @Query("cmt") float jumpFromToSecAlt2,        
            @Query("cpn") String clientPlaybackNonce,     
            @Query("ei") String eventId,                  
            @Query("vm") String vm,                       
            @Query("of") String of                        
    );

     
    @GET("https://www.youtube.com/api/stats/watchtime?ns=yt&ver=2&final=1")
    Call<WatchTimeEmptyResult> updateWatchTimeShort(
            @Query("docid") String videoId,               
            @Query("st") float jumpFromToSec,             
            @Query("et") float jumpFromToSecAlt,          
            @Query("cpn") String clientPlaybackNonce,     
            @Query("ei") String eventId,                  
            @Query("vm") String vm,                       
            @Query("of") String of                        
    );
}
