package com.liskovsoft.youtubeapi.service.data;

import com.liskovsoft.mediaserviceinterfaces.data.MediaFormat;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemStoryboard;
import com.liskovsoft.mediaserviceinterfaces.data.MediaSubtitle;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.sharedutils.rx.RxHelper;
import com.liskovsoft.youtubeapi.app.AppService;
import com.liskovsoft.youtubeapi.app.PoTokenGate;
import com.liskovsoft.youtubeapi.common.helpers.AppClient;
import com.liskovsoft.youtubeapi.formatbuilders.hlsbuilder.YouTubeUrlListBuilder;
import com.liskovsoft.youtubeapi.formatbuilders.mpdbuilder.YouTubeMPDBuilder;
import com.liskovsoft.youtubeapi.formatbuilders.storyboard.YouTubeStoryParser;
import com.liskovsoft.youtubeapi.formatbuilders.storyboard.YouTubeStoryParser.Storyboard;
import com.liskovsoft.youtubeapi.videoinfo.models.CaptionTrack;
import com.liskovsoft.youtubeapi.videoinfo.models.VideoDetails;
import com.liskovsoft.youtubeapi.videoinfo.models.VideoInfo;
import com.liskovsoft.youtubeapi.videoinfo.models.formats.AdaptiveVideoFormat;
import com.liskovsoft.youtubeapi.videoinfo.models.formats.RegularVideoFormat;
import io.reactivex.Observable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class YouTubeMediaItemFormatInfo implements MediaItemFormatInfo {
    private static final String TAG = YouTubeMediaItemFormatInfo.class.getSimpleName();
    private String mLengthSeconds;
    private String mTitle;
    private String mAuthor;
    private String mViewCount;
    private String mDescription;
    private String mVideoId;
    private String mChannelId;
    private boolean mIsLive;
    private boolean mIsLiveContent;
    private boolean mIsLowLatencyLiveStream;
    private boolean mIsStreamSeekable;
    private List<MediaFormat> mAdaptiveFormats;
    private List<MediaFormat> mUrlFormats;
    private List<MediaSubtitle> mSubtitles;
    private String mDashManifestUrl;
    private String mHlsManifestUrl;
    private String mEventId;  
    private String mVisitorMonitoringData;  
    private String mOfParam;  
    private String mStoryboardSpec;
    private boolean mIsUnplayable;
    private String mPlayabilityStatus;
    private String mStartTimestamp;
    private String mUploadDate;
    private long mStartTimeMs;
    private int mStartSegmentNum;
    private int mSegmentDurationUs;
    private boolean mHasExtendedHlsFormats;
    private float mLoudnessDb;
    private boolean mContainsAdaptiveVideoFormats;
    private boolean mIsAuth;
    private boolean mIsSynced;
    private boolean mIsUnknownError;
    private String mPaidContentText;
    private String mClickTrackingParams;
    private String mVideoPlaybackUstreamerConfig;
    private String mServerAbrStreamingUrl;
    private String mPoToken;
    private AppClient mClient;
    private static final Pattern durationPattern1 = Pattern.compile("dur=([^&]*)");
    private static final Pattern durationPattern2 = Pattern.compile("/dur/([^/]*)");

    private YouTubeMediaItemFormatInfo() {
        
    }

    public static YouTubeMediaItemFormatInfo from(VideoInfo videoInfo) {
        if (videoInfo == null) {
            return null;
        }

        YouTubeMediaItemFormatInfo formatInfo = new YouTubeMediaItemFormatInfo();

        if (videoInfo.getAdaptiveFormats() != null) {
            formatInfo.mContainsAdaptiveVideoFormats = videoInfo.containsAdaptiveVideoInfo();

            formatInfo.mAdaptiveFormats = new ArrayList<>();

            for (AdaptiveVideoFormat format : videoInfo.getAdaptiveFormats()) {
                formatInfo.mAdaptiveFormats.add(YouTubeMediaFormat.from(format));
            }
        }

        if (videoInfo.getRegularFormats() != null) {
            formatInfo.mUrlFormats = new ArrayList<>();

            for (RegularVideoFormat format : videoInfo.getRegularFormats()) {
                formatInfo.mUrlFormats.add(YouTubeMediaFormat.from(format));
            }
        }

        VideoDetails videoDetails = videoInfo.getVideoDetails();

        if (videoDetails != null) {
            formatInfo.mLengthSeconds = videoDetails.getLengthSeconds();
            formatInfo.mVideoId = videoDetails.getVideoId();
            formatInfo.mViewCount = videoDetails.getViewCount();
            formatInfo.mTitle = videoDetails.getTitle();
            formatInfo.mDescription = videoDetails.getShortDescription();
            formatInfo.mChannelId = videoDetails.getChannelId();
            formatInfo.mAuthor = videoDetails.getAuthor();
            formatInfo.mIsLive = videoDetails.isLive();
            formatInfo.mIsLiveContent = videoDetails.isLiveContent();
            formatInfo.mIsLowLatencyLiveStream = videoDetails.isLowLatencyLiveStream();
        }

        formatInfo.mDashManifestUrl = videoInfo.getDashManifestUrl();
        formatInfo.mHlsManifestUrl = videoInfo.getHlsManifestUrl();
         
        formatInfo.mEventId = videoInfo.getEventId();
        formatInfo.mVisitorMonitoringData = videoInfo.getVisitorMonitoringData();
        formatInfo.mOfParam = videoInfo.getOfParam();
         
        formatInfo.mStoryboardSpec = videoInfo.getStoryboardSpec();
        formatInfo.mIsUnplayable = videoInfo.isUnplayable();
        formatInfo.mIsAuth = videoInfo.isAuth();
        formatInfo.mIsUnknownError = videoInfo.isUnknownRestricted();
        formatInfo.mPlayabilityStatus = videoInfo.getPlayabilityStatus();
        formatInfo.mIsStreamSeekable = videoInfo.isHfr() || videoInfo.isStreamSeekable();
        formatInfo.mStartTimestamp = videoInfo.getStartTimestamp();
        formatInfo.mUploadDate = videoInfo.getUploadDate();
        formatInfo.mStartTimeMs = videoInfo.getStartTimeMs();
        formatInfo.mStartSegmentNum = videoInfo.getStartSegmentNum();
        formatInfo.mSegmentDurationUs = videoInfo.getSegmentDurationUs();
        formatInfo.mHasExtendedHlsFormats = videoInfo.hasExtendedHlsFormats();
        formatInfo.mLoudnessDb = videoInfo.getLoudnessDb();
        formatInfo.mPaidContentText = videoInfo.getPaidContentText();
        formatInfo.mVideoPlaybackUstreamerConfig = videoInfo.getVideoPlaybackUstreamerConfig();
        formatInfo.mServerAbrStreamingUrl = videoInfo.getServerAbrStreamingUrl();
        formatInfo.mPoToken = videoInfo.getPoToken();
        formatInfo.mClient = videoInfo.getClient();

        List<CaptionTrack> captionTracks = videoInfo.getCaptionTracks();

        if (captionTracks != null) {
            formatInfo.mSubtitles = new ArrayList<>();

            for (CaptionTrack track : captionTracks) {
                formatInfo.mSubtitles.add(YouTubeMediaSubtitle.from(track));
            }
        }

        return formatInfo;
    }

    @Override
    public List<MediaFormat> getAdaptiveFormats() {
        return mAdaptiveFormats;
    }

    @Override
    public List<MediaFormat> getUrlFormats() {
        return mUrlFormats;
    }

    @Override
    public List<MediaSubtitle> getSubtitles() {
        return mSubtitles;
    }

    @Override
    public String getLengthSeconds() {
        ensureLengthIsSet();

        return mLengthSeconds;
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    @Override
    public String getAuthor() {
        return mAuthor;
    }

    @Override
    public String getViewCount() {
        return mViewCount;
    }

    @Override
    public String getDescription() {
        return mDescription;
    }

    @Override
    public String getVideoId() {
        return mVideoId;
    }

    @Override
    public String getChannelId() {
        return mChannelId;
    }

    @Override
    public boolean isLive() {
        return mIsLive;
    }

    @Override
    public boolean isLiveContent() {
        return mIsLiveContent;
    }
    
    public boolean isLowLatencyStream() {
        return mIsLowLatencyLiveStream;
    }

    @Override
    public boolean containsMedia() {
        return containsDashUrl() || containsHlsUrl() || containsAdaptiveVideoFormats() || containsUrlFormats();
    }

    @Override
    public boolean containsSabrFormats() {
        return mContainsAdaptiveVideoFormats && mAdaptiveFormats.get(0).getFormatType() == MediaFormat.FORMAT_TYPE_SABR;
    }

    @Override
    public boolean containsDashFormats() {
        return mContainsAdaptiveVideoFormats && mAdaptiveFormats.get(0).getFormatType() == MediaFormat.FORMAT_TYPE_DASH;
    }
    
    private boolean containsAdaptiveVideoFormats() {
        return mContainsAdaptiveVideoFormats;
    }

    @Override
    public boolean containsHlsUrl() {
        return mHlsManifestUrl != null;
    }

    @Override
    public boolean containsDashUrl() {
        return mDashManifestUrl != null;
    }

    @Override
    public boolean containsUrlFormats() {
        return mUrlFormats != null;
    }

    @Override
    public boolean hasExtendedHlsFormats() {
        return mHasExtendedHlsFormats;
    }

    @Override
    public float getVolumeLevel() {
        float result = 1.0f;  

         
         
         
         
         
         
         

         
         
         
         
         
         
         
         
         

         
         
         
         
         
         
         

        if (mLoudnessDb != 0) {
             
             
             
            float normalLevel = (float) Math.pow(10.0f, mLoudnessDb / 20.0f);
            if (normalLevel > 1.95) {  
                 
                 
                normalLevel = 1.5f;
            }
             
            result = 2.0f - normalLevel;
        }

        return result / 2;
    }

    @Override
    public String getHlsManifestUrl() {
        return mHlsManifestUrl;
    }

    @Override
    public String getDashManifestUrl() {
        return mDashManifestUrl;
    }

    @Override
    public InputStream createMpdStream() {
        return YouTubeMPDBuilder.from(this).build();
    }

    @Override
    public Observable<InputStream> createMpdStreamObservable() {
        return RxHelper.fromCallable(this::createMpdStream);
    }

    @Override
    public List<String> createUrlList() {
        return YouTubeUrlListBuilder.from(this).buildUriList();
    }

    @Override
    public MediaItemStoryboard createStoryboard() {
        if (mStoryboardSpec == null) {
            return null;
        }

        YouTubeStoryParser storyParser = YouTubeStoryParser.from(mStoryboardSpec);
        storyParser.setSegmentDurationUs(getSegmentDurationUs());
         
        storyParser.setStartSegmentNum(getStartSegmentNum());
        Storyboard storyboard = storyParser.extractStory();

        return YouTubeMediaItemStoryboard.from(storyboard);
    }

    @Override
    public boolean isUnplayable() {
        return mIsUnplayable;
    }

    public boolean isAuth() {
        return mIsAuth;
    }

    public boolean isSynced() {
        return mIsSynced;
    }

    @Override
    public boolean isUnknownError() {
        return mIsUnknownError;
    }

    @Override
    public String getPlayabilityStatus() {
        return mPlayabilityStatus;
    }

    @Override
    public boolean isStreamSeekable() {
        return mIsStreamSeekable;
    }

    @Override
    public String getStartTimestamp() {
        return mStartTimestamp;
    }

    @Override
    public String getUploadDate() {
        return mUploadDate;
    }

    @Override
    public long getStartTimeMs() {
        return mStartTimeMs;
    }

    @Override
    public int getStartSegmentNum() {
        return mStartSegmentNum;
    }

    @Override
    public int getSegmentDurationUs() {
        return mSegmentDurationUs;
    }

    @Override
    public String getPaidContentText() {
        return mPaidContentText;
    }

    @Override
    public String getVideoPlaybackUstreamerConfig() {
        return mVideoPlaybackUstreamerConfig;
    }

    @Override
    public String getServerAbrStreamingUrl() {
        return mServerAbrStreamingUrl;
    }

    @Override
    public String getPoToken() {
        return mPoToken;
    }

    @Override
    public ClientInfo getClientInfo() {
        return mClient;
    }

    public String getEventId() {
        return mEventId;
    }

    public String getVisitorMonitoringData() {
        return mVisitorMonitoringData;
    }

    public String getOfParam() {
        return mOfParam;
    }

    public String getClickTrackingParams() {
        return mClickTrackingParams;
    }

    public void setClickTrackingParams(String clickTrackingParams) {
        mClickTrackingParams = clickTrackingParams;
    }

     
    public boolean isCacheActual() {
         

         
         
         
        return containsMedia() && AppService.instance().isPlayerCacheActual() && !PoTokenGate.isWebPotExpired();
    }

     
    public void sync(YouTubeMediaItemFormatInfo formatInfo) {
        mIsSynced = true;

        if (formatInfo == null || Helpers.anyNull(formatInfo.getEventId(), formatInfo.getVisitorMonitoringData(), formatInfo.getOfParam())) {
            return;
        }

         
        mEventId = formatInfo.getEventId();
        mVisitorMonitoringData = formatInfo.getVisitorMonitoringData();
        mOfParam = formatInfo.getOfParam();
        mIsAuth = formatInfo.isAuth();
    }

     
    private void ensureLengthIsSet() {
        if (mLengthSeconds != null) {
            return;
        }

         
        mLengthSeconds = extractDurationFromTrack();
    }

     
    private String extractDurationFromTrack() {
        if (mAdaptiveFormats == null && mUrlFormats == null) {
            return null;
        }

        String url = null;
         
        List<MediaFormat> videos = mAdaptiveFormats != null ? mAdaptiveFormats : mUrlFormats;
        for (MediaFormat item : videos) {
            url = item.getUrl();
            break;  
        }
        String result = Helpers.runMultiMatcher(url, durationPattern1, durationPattern2);

        if (result == null) {
             
            Log.e(TAG, "Videos in the list doesn't have a duration. Content: " + videos);
        }

        return result;
    }

     
    private void fixOTF(YouTubeMediaFormat mediaItem) {
        if (mediaItem.isOtf()) {
            if (mediaItem.getUrl() != null) {
                 
                mediaItem.setUrl(mediaItem.getUrl() + "&sq=7");
                 
                 
                 
            }
        }
    }
}
