package com.liskovsoft.youtubeapi.playlist;

import com.liskovsoft.youtubeapi.actions.models.ActionResult;
import com.liskovsoft.googlecommon.common.converters.jsonpath.WithJsonPath;
import com.liskovsoft.youtubeapi.playlist.models.PlaylistsResult;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

 
@WithJsonPath
public interface PlaylistApi {
    @Headers("Content-Type: application/json")
    @POST("https://www.youtube.com/youtubei/v1/playlist/get_add_to_playlist")
    Call<PlaylistsResult> getPlaylistsInfo(@Body String playlistsInfoQuery);

    @Headers("Content-Type: application/json")
    @POST("https://www.youtube.com/youtubei/v1/browse/edit_playlist")
    Call<ActionResult> editPlaylist(@Body String editPlaylistQuery);

     
    @Headers("Content-Type: application/json")
    @POST("https://www.youtube.com/youtubei/v1/like/like")
    Call<ActionResult> saveForeignPlaylist(@Body String saveRemoveForeignPlaylistQuery);

     
    @Headers("Content-Type: application/json")
    @POST("https://www.youtube.com/youtubei/v1/like/removelike")
    Call<ActionResult> removeForeignPlaylist(@Body String saveRemoveForeignPlaylistQuery);

    @Headers("Content-Type: application/json")
    @POST("https://www.youtube.com/youtubei/v1/playlist/create")
    Call<ActionResult> createPlaylist(@Body String createPlaylistQuery);

     
    @Headers("Content-Type: application/json")
    @POST("https://www.youtube.com/youtubei/v1/playlist/delete")
    Call<ActionResult> removePlaylist(@Body String removePlaylistQuery);
}
