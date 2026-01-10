package com.liskovsoft.mediaserviceinterfaces.data;

import android.media.Rating;

public interface MediaItem {
    int TYPE_UNDEFINED = -1;
    int TYPE_VIDEO = 0;
    int TYPE_MUSIC = 1;
    int TYPE_CHANNEL = 2;
    int TYPE_PLAYLIST = 3;

    int getType();

     
    boolean isLive();
    boolean isUpcoming();
    boolean isShorts();
    int getPercentWatched();
    int getStartTimeSeconds();
    String getAuthor();
    String getFeedbackToken();
    String getFeedbackToken2();

     
    String getPlaylistId();
    int getPlaylistIndex();
    
    String getParams();  
    String getReloadPageKey();  

     
    boolean hasNewContent();

     
    int getId();
    String getTitle();
     
    CharSequence getSecondTitle();
    String getVideoId();
     
    String getContentType();
     
    long getDurationMs();
    String getBadgeText();
    String getProductionDate();
    long getPublishedDate();
    String getCardImageUrl();
    String getBackgroundImageUrl();
    int getWidth();
    int getHeight();
    String getChannelId();
    String getVideoPreviewUrl();
     
    String getAudioChannelConfig();
     
    String getPurchasePrice();
    String getRentalPrice();
     
    int getRatingStyle();
     
    double getRatingScore();
    boolean isMovie();
    boolean hasUploads();
    String getClickTrackingParams();
    String getSearchQuery();
}
