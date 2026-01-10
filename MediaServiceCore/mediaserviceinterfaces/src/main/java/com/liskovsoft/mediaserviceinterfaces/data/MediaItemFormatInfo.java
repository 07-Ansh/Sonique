package com.liskovsoft.mediaserviceinterfaces.data;

import io.reactivex.Observable;

import java.io.InputStream;
import java.util.List;

public interface MediaItemFormatInfo {
    List<MediaFormat> getAdaptiveFormats();
    List<MediaFormat> getUrlFormats();
    List<MediaSubtitle> getSubtitles();
    String getHlsManifestUrl();
    String getDashManifestUrl();
     
    String getLengthSeconds();
    String getTitle();
    String getAuthor();
    String getViewCount();
    String getDescription();
    String getVideoId();
    String getChannelId();
    boolean isLive();
    boolean isLiveContent();
    boolean containsMedia();
    boolean containsSabrFormats();
    boolean containsDashFormats();
    boolean containsHlsUrl();
    boolean containsDashUrl();
    boolean containsUrlFormats();
    boolean hasExtendedHlsFormats();
    float getVolumeLevel();
    InputStream createMpdStream();
    Observable<InputStream> createMpdStreamObservable();
    List<String> createUrlList();
    MediaItemStoryboard createStoryboard();
    boolean isUnplayable();
    boolean isUnknownError();
    String getPlayabilityStatus();
    boolean isStreamSeekable();
     
    String getStartTimestamp();
    String getUploadDate();
     
    long getStartTimeMs();
     
    int getStartSegmentNum();
     
    int getSegmentDurationUs();
    String getPaidContentText();
    String getVideoPlaybackUstreamerConfig();
    String getServerAbrStreamingUrl();
    String getPoToken();
    ClientInfo getClientInfo();

    interface ClientInfo {
        String getClientName();
        String getClientVersion();
    }
}
