package com.liskovsoft.youtubeapi.formatbuilders.mpdbuilder;

import android.util.Xml;
import com.liskovsoft.mediaserviceinterfaces.data.MediaFormat;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo;
import com.liskovsoft.mediaserviceinterfaces.data.MediaSubtitle;
import com.liskovsoft.sharedutils.helpers.FileHelpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.youtubeapi.formatbuilders.mpdbuilder.YouTubeOtfSegmentParser.OtfSegment;
import com.liskovsoft.youtubeapi.formatbuilders.utils.ITagUtils;
import com.liskovsoft.youtubeapi.formatbuilders.utils.MediaFormatUtils;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

 
public class YouTubeMPDBuilder implements MPDBuilder {
    private static final String NULL_INDEX_RANGE = "0-0";
    private static final String NULL_CONTENT_LENGTH = "0";
    private static final String TAG = YouTubeMPDBuilder.class.getSimpleName();
    private static final int MAX_DURATION_SEC = 48 * 60 * 60;
    private final MediaItemFormatInfo mInfo;
    private XmlSerializer mXmlSerializer;
    private StringWriter mWriter;
    private int mId;
    private final Set<MediaFormat> mMP4Videos;
    private final Set<MediaFormat> mWEBMVideos;
    private final Map<String, Set<MediaFormat>> mMP4Audios;
    private final Map<String, Set<MediaFormat>> mWEBMAudios;
    private final List<MediaSubtitle> mSubs;
    private final YouTubeOtfSegmentParser mSegmentParser;
    private String mLimitVideoCodec;
    private String mLimitAudioCodec;

    private YouTubeMPDBuilder(MediaItemFormatInfo info) {
        mInfo = info;
        MediaFormatComparator comp = new MediaFormatComparator();
        mMP4Videos = new TreeSet<>(comp);
        mWEBMVideos = new TreeSet<>(comp);
        mMP4Audios = new HashMap<>();
        mWEBMAudios = new HashMap<>();
        mSubs = new ArrayList<>();
        mSegmentParser = new YouTubeOtfSegmentParser(true);

        initXmlSerializer();
    }

    public static MPDBuilder from(MediaItemFormatInfo formatInfo) {
        MPDBuilder builder = new YouTubeMPDBuilder(formatInfo);

        if (formatInfo.containsDashFormats()) {
            for (MediaFormat format : formatInfo.getAdaptiveFormats()) {
                builder.append(format);
            }

            if (formatInfo.getSubtitles() != null) {
                builder.append(formatInfo.getSubtitles());
            }
        }

        return builder;
    }

    private void initXmlSerializer() {
        mXmlSerializer = Xml.newSerializer();
        mWriter = new StringWriter();

        setOutput(mXmlSerializer, mWriter);

        startDocument(mXmlSerializer);
        mXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
    }

    private void writePrologue() {
        String duration = mInfo.getLengthSeconds();
        String durationParam = String.format("PT%sS", duration);

        startTag("", "MPD");
        attribute("", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        attribute("", "xmlns", "urn:mpeg:DASH:schema:MPD:2011");
        attribute("", "xmlns:yt", "http://youtube.com/yt/2012/10/10");
        attribute("", "xsi:schemaLocation", "urn:mpeg:DASH:schema:MPD:2011 DASH-MPD.xsd");
        attribute("", "minBufferTime", "PT1.500S");

         
        if (isLive()) {
            attribute("", "profiles", "urn:mpeg:dash:profile:isoff-on-demand:2011");  
            attribute("", "type", "dynamic");
            attribute("", "minimumUpdatePeriod", "P100Y");  

             
             
             
             
             
             
             
             
             
             

             
             
             
             
             
             
             
             
        } else {
            attribute("", "profiles", "urn:mpeg:dash:profile:isoff-on-demand:2011");
            attribute("", "type", "static");
            attribute("", "mediaPresentationDuration", durationParam);
        }


        startTag("", "Period");

        if (isLive()) {
             
            attribute("", "start", "PT0S");  
             
             
             
        } else {
            attribute("", "duration", durationParam);
        }
    }

    private void writeEpilogue() {
        endTag("", "Period");
        endTag("", "MPD");
        endDocument();
    }

    private void writeMediaTags() {
        if (isLive()) {
            writeLiveHeaderSegmentList();
        }

         
        writeMediaTagsForGroup(mMP4Videos);
        writeMediaTagsForGroup(mWEBMVideos);

        for (Set<MediaFormat> formats : mMP4Audios.values()) {
            writeMediaTagsForGroup(formats);
        }

        for (Set<MediaFormat> formats : mWEBMAudios.values()) {
            writeMediaTagsForGroup(formats);
        }
        
        writeMediaTagsForGroup(mSubs);
    }

    private void writeLiveHeaderSegmentList() {
         
         
         
         
         
         
         
         
         
         
         
         
         
         
         
         
         
         
         
    }

    private void writeMediaTagsForGroup(List<MediaSubtitle> subs) {
        if (subs.size() == 0) {
            return;
        }

        for (MediaSubtitle sub : subs) {
            writeMediaListPrologue(sub);

            writeMediaFormatTag(sub);

            writeMediaListEpilogue();
        }
    }

    private void writeMediaTagsForGroup(Set<MediaFormat> items) {
        if (items.size() == 0) {
            return;
        }

        List<MediaFormat> filtered = filterOtfItems(items);

        if (filtered.size() == 0) {
            return;
        }

        MediaFormat firstItem = null;
        for (MediaFormat item : filtered) {
            firstItem = item;
            break;
        }

        writeMediaListPrologue(String.valueOf(mId++), MediaFormatUtils.extractMimeType(firstItem), firstItem.getLanguage());

         
        for (MediaFormat item : filtered) {
            if (mLimitVideoCodec != null && isVideo(item) && !item.getMimeType().contains(mLimitVideoCodec)) {
                continue;
            }

            if (mLimitAudioCodec != null && isAudio(item) && !item.getMimeType().contains(mLimitAudioCodec)) {
                continue;
            }

            if (item.getGlobalSegmentList() != null) {
                writeGlobalSegmentList(item);
                continue;
            }

            writeMediaFormatTag(item);
        }

        writeMediaListEpilogue();
    }

    private void writeGlobalSegmentList(MediaFormat format) {
        startTag("", "SegmentList");

        attribute("", "startNumber", "0");
        attribute("", "timescale", "1000");

        startTag("", "SegmentTimeline");

         
        for (String segment : format.getGlobalSegmentList()) {
            startTag("", "S");
            attribute("", "d", segment);
            endTag("", "S");
        }

        endTag("", "SegmentTimeline");

        endTag("", "SegmentList");
    }

    private XmlSerializer attribute(String namespace, String name, String value) {
        if (value == null) {
            return mXmlSerializer;
        }
        try {
            return mXmlSerializer.attribute(namespace, name, value);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private XmlSerializer startTag(String namespace, String name) {
        try {
            return mXmlSerializer.startTag(namespace, name);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void setOutput(XmlSerializer xmlSerializer, StringWriter writer) {
        try {
            xmlSerializer.setOutput(writer);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void startDocument(XmlSerializer xmlSerializer) {
        try {
            xmlSerializer.startDocument("UTF-8", true);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void endDocument() {
        try {
            mXmlSerializer.endDocument();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void endTag(String namespace, String name) {
        try {
            mXmlSerializer.endTag(namespace, name);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void writeMediaListPrologue(String id, String mimeType, String language) {
        startTag("", "AdaptationSet");
        attribute("", "id", id);
        attribute("", "mimeType", mimeType);
        if (language != null) {
            attribute("", "lang", language);
        }
        attribute("", "subsegmentAlignment", "true");

        startTag("", "Role");
        attribute("", "schemeIdUri", "urn:mpeg:DASH:role:2011");
        attribute("", "value", "main");
        endTag("", "Role");
    }

    private void writeMediaListPrologue(MediaSubtitle sub) {
        String id = String.valueOf(mId++);

        startTag("", "AdaptationSet");
        attribute("", "id", id);
        attribute("", "mimeType", sub.getMimeType());
        attribute("", "lang", sub.getName() == null ? sub.getLanguageCode() : sub.getName());

        startTag("", "Role");
        attribute("", "schemeIdUri", "urn:mpeg:DASH:role:2011");
        attribute("", "value", "subtitle");
        endTag("", "Role");
    }

    private void writeMediaListEpilogue() {
        endTag("", "AdaptationSet");
    }

    @Override
    public void append(MediaFormat mediaItem) {
        if (!MediaFormatUtils.checkMediaUrl(mediaItem)) {
            Log.e(TAG, "Media item doesn't contain required url field!");
            return;
        }

         
        if (!MediaFormatUtils.isDash(mediaItem)) {
            return;
        }

         

        Set<MediaFormat> placeholder = null;
        String mimeType = MediaFormatUtils.extractMimeType(mediaItem);
        if (mimeType != null) {
            switch (mimeType) {
                case MediaFormatUtils.MIME_MP4_VIDEO:
                    placeholder = mMP4Videos;
                    break;
                case MediaFormatUtils.MIME_WEBM_VIDEO:
                    placeholder = mWEBMVideos;
                    break;
                case MediaFormatUtils.MIME_MP4_AUDIO:
                    placeholder = getMP4Audios(mediaItem.getLanguage());
                    break;
                case MediaFormatUtils.MIME_WEBM_AUDIO:
                    placeholder = getWEBMAudios(mediaItem.getLanguage());
                    break;
            }
        }

        if (placeholder != null) {
            placeholder.add(mediaItem);  
        }
    }

    @Override
    public void append(List<MediaSubtitle> subs) {
        mSubs.addAll(subs);
    }

    @Override
    public void append(MediaSubtitle sub) {
        mSubs.add(sub);
    }

    private void writeMediaFormatTag(MediaFormat format) {
        startTag("", "Representation");

        attribute("", "id", format.isDrc() ? format.getITag() + "-drc" : format.getITag());
        attribute("", "codecs", MediaFormatUtils.extractCodecs(format));
        attribute("", "startWithSAP", "1");
        attribute("", "bandwidth", format.getBitrate());

        if (isVideo(format)) {
             
            attribute("", "width", String.valueOf(format.getWidth()));
            attribute("", "height", String.valueOf(format.getHeight()));
            attribute("", "maxPlayoutRate", "1");
            attribute("", "frameRate", format.getFps());
        } else {
             
            attribute("", "audioSamplingRate", ITagUtils.getAudioRateByTag(format.getITag()));
        }

        if (format.isOtf()) {
            writeOtfSegmentTemplate(format);
        } else {
            startTag("", "BaseURL");

            if (format.getClen() != null && !format.getClen().equals(NULL_CONTENT_LENGTH)) {
                attribute("", "yt:contentLength", format.getClen());
            }

            text(format.getUrl());

            endTag("", "BaseURL");
        }

         
        if (isLive()) {
            writeLiveMediaSegmentList(format);
        } else if (format.getSegmentUrlList() != null) {
            writeSegmentList(format);
        } else if (format.getIndex() != null &&
                !format.getIndex().equals(NULL_INDEX_RANGE)) {  
            writeSegmentBase(format);
        }

        endTag("", "Representation");
    }

    private void writeSegmentBase(MediaFormat item) {
         
        startTag("", "SegmentBase");

        if (item.getIndex() != null) {
            attribute("", "indexRange", item.getIndex());
            attribute("", "indexRangeExact", "true");
        }

        startTag("", "Initialization");

        attribute("", "range", item.getInit());

        endTag("", "Initialization");

        endTag("", "SegmentBase");
    }

    private void writeSegmentList(MediaFormat item) {
        startTag("", "SegmentList");

         
        if (item.getSourceUrl() != null) {
            startTag("", "Initialization");
            attribute("", "sourceURL", item.getSourceUrl());
            endTag("", "Initialization");
        }

         
        for (String url : item.getSegmentUrlList()) {
            startTag("", "SegmentURL");
            attribute("", "media", url);
            endTag("", "SegmentURL");
        }

        endTag("", "SegmentList");
    }

    private void writeLiveMediaSegmentList(MediaFormat format) {
         
         
         
         
         
         
         
         
         
         
         
         
         
         

         
         
         

        int unitsPerSecond = 1_000_000;

         
        int segmentDurationUs = mInfo.getSegmentDurationUs();

        if (segmentDurationUs <= 0) {
             
            segmentDurationUs = format.getTargetDurationSec() * 1_000_000;
        }

        int lengthSeconds = Integer.parseInt(mInfo.getLengthSeconds());

        if (mInfo.isLive() || lengthSeconds <= 0) {
             
            lengthSeconds = MAX_DURATION_SEC;
        }

         
         
        int segmentDurationUnits = (int)(segmentDurationUs * (long) unitsPerSecond / 1_000_000);
         
         
         
        int segmentCount = (int) Math.ceil(lengthSeconds * (double) unitsPerSecond / segmentDurationUnits);
         
         
         
        long offsetUnits = (long) segmentDurationUnits * mInfo.getStartSegmentNum();

        String segmentDurationUnitsStr = String.valueOf(segmentDurationUnits);
         
        String offsetUnitsStr = String.valueOf(offsetUnits);

        startTag("", "SegmentTemplate");

        attribute("", "duration", segmentDurationUnitsStr);  
        attribute("", "timescale", String.valueOf(unitsPerSecond));  
        attribute("", "media", format.getUrl() + "&sq=$Number$");
        attribute("", "startNumber", String.valueOf(mInfo.getStartSegmentNum()));
         
        attribute("", "presentationTimeOffset", offsetUnitsStr);  
         

         

         
         
         
         

        startTag("", "SegmentTimeline");

        startTag("", "S");  

        attribute("", "t", offsetUnitsStr);  
        attribute("", "d", segmentDurationUnitsStr);  
        attribute("", "r", String.valueOf(segmentCount));  

        endTag("", "S");

        endTag("", "SegmentTimeline");

        endTag("", "SegmentTemplate");
    }

    private void writeMediaFormatTag(MediaSubtitle sub) {
        String bandwidth = "268";

        startTag("", "Representation");

        attribute("", "id", String.valueOf(mId));

        attribute("", "bandwidth", bandwidth);

        attribute("", "codecs", sub.getCodecs());

        startTag("", "BaseURL");

        text(sub.getBaseUrl());

        endTag("", "BaseURL");

        endTag("", "Representation");
    }

    private boolean isVideo(MediaFormat item) {
        return item.getWidth() > 0 && item.getHeight() > 0;
    }

    private boolean isAudio(MediaFormat item) {
        return item.getMimeType() != null && item.getMimeType().contains("audio");
    }

    private XmlSerializer text(String url) {
        try {
            return mXmlSerializer.text(url);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

     
    private boolean ensureRequiredFieldsAreSet() {
        return ensureLengthIsSet();
    }

     
    private boolean ensureLengthIsSet() {
        if (mInfo == null) {
             
            Log.e(TAG, "MediaItemDetails not initialized");
            return false;
        }

        if (mInfo.getLengthSeconds() == null) {
            Log.e(TAG, "FormatInfo doesn't contain duration");
            return false;
        }
        
        return true;
    }

    @Override
    public InputStream build() {
        if (!mInfo.containsDashFormats()) {
            return null;
        }

        if (ensureRequiredFieldsAreSet()) {
            writePrologue();

            writeMediaTags();

            writeEpilogue();

            return FileHelpers.toStream(mWriter.toString());
        }

        return null;
    }

    @Override
    public boolean isEmpty() {
        return (mMP4Videos.size() == 0 && mWEBMVideos.size() == 0
                && mMP4Audios.size() == 0 && mWEBMAudios.size() == 0) || !ensureRequiredFieldsAreSet();
    }

    @Override
    public boolean isDynamic() {
        return isLive();
    }

    @Override
    public void limitVideoCodec(String codec) {
        mLimitVideoCodec = codec;
    }

    @Override
    public void limitAudioCodec(String codec) {
        mLimitAudioCodec = codec;
    }

    private boolean isLive() {
        for (MediaFormat item : mMP4Videos) {
            return MediaFormatUtils.isLiveMedia(item);
        }

        for (MediaFormat item : mWEBMVideos) {
            return MediaFormatUtils.isLiveMedia(item);
        }

        return false;
    }

     
    private void writeOtfSegmentTemplateOld(MediaFormat item) {
         
         
         
         
         
         

        startTag("", "SegmentTemplate");

        attribute("", "timescale", "1000");  
        attribute("", "duration", "5100");  
        attribute("", "media", item.getUrl() + "&sq=$Number$");
        attribute("", "initialization", item.getUrl() + "&sq=0");  
        attribute("", "startNumber", "1");

        endTag("", "SegmentTemplate");
    }

    private void writeOtfSegmentTemplate(MediaFormat item) {
         
         
         
         
         
         

        List<OtfSegment> segments = mSegmentParser.parse(item.getOtfInitUrl());

        writeOtfSegmentTemplate(item.getOtfTemplateUrl(), item.getOtfInitUrl(), "1", segments);
    }

    private void writeOtfSegmentTemplate(String mediaUrl, String initUrl, String startNumber, List<OtfSegment> segments) {
        if (segments != null && segments.size() > 0) {
            startTag("", "SegmentTemplate");

            attribute("", "timescale", "1000");  
            attribute("", "media", mediaUrl);
            attribute("", "initialization", initUrl);
            attribute("", "startNumber", startNumber);

            writeOtfSegmentTimeline(segments);

            endTag("", "SegmentTemplate");
        }
    }

    private void writeOtfSegmentTimeline(List<OtfSegment> segments) {
        if (segments != null && segments.size() > 0) {
            startTag("", "SegmentTimeline");

            int totalTime = 0;

            for (OtfSegment segment : segments) {
                startTag("", "S");  

                attribute("", "t", String.valueOf(totalTime));  
                attribute("", "d", segment.getDuration());  

                attribute("", "r", segment.getRepeatCount());  

                endTag("", "S");

                int segmentDuration = Integer.parseInt(segment.getDuration());
                int segmentRepeatCount = Integer.parseInt(segment.getRepeatCount()) + 1;  

                totalTime = totalTime + (segmentRepeatCount * segmentDuration);
            }

            endTag("", "SegmentTimeline");
        }
    }

     
    private List<MediaFormat> filterOtfItems(Set<MediaFormat> items) {
        List<MediaFormat> result = new ArrayList<>();

        for (MediaFormat item : items) {
            if (item.isOtf() && mSegmentParser.parse(item.getOtfInitUrl()) == null) {
                continue;
            }

            result.add(item);
        }

        return result;
    }

    private Set<MediaFormat> getMP4Audios(String language) {
        return getFormats(mMP4Audios, language);
    }

    private Set<MediaFormat> getWEBMAudios(String language) {
        return getFormats(mWEBMAudios, language);
    }

    private static Set<MediaFormat> getFormats(Map<String, Set<MediaFormat>> formatMap, String language) {
        if (language == null) {
            language = "default";
        }

        Set<MediaFormat> mediaFormats = formatMap.get(language);

        if (mediaFormats == null) {
            mediaFormats = new TreeSet<>(new MediaFormatComparator());
            formatMap.put(language, mediaFormats);
        }

        return mediaFormats;
    }
}
