package com.liskovsoft.youtubeapi.app.potokennp2.misc;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

 
public final class PoTokenResult {
    @Nonnull
    public final String videoId;

     
    @Nonnull
    public final String visitorData;

     
    @Nonnull
    public final String playerRequestPoToken;

     
    @Nullable
    public final String streamingDataPoToken;

     
    public PoTokenResult(@Nonnull final String videoId,
                         @Nonnull final String visitorData,
                         @Nonnull final String playerRequestPoToken,
                         @Nullable final String streamingDataPoToken) {
        this.videoId = Objects.requireNonNull(videoId);
        this.visitorData = Objects.requireNonNull(visitorData);
        this.playerRequestPoToken = Objects.requireNonNull(playerRequestPoToken);
        this.streamingDataPoToken = streamingDataPoToken;
    }
}
