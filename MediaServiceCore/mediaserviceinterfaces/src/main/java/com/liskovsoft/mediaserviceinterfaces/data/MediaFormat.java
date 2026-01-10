package com.liskovsoft.mediaserviceinterfaces.data;

import java.util.List;

public interface MediaFormat extends Comparable<MediaFormat> {
    int FORMAT_TYPE_DASH = 0;
    int FORMAT_TYPE_REGULAR = 1;
    int FORMAT_TYPE_SABR = 2;
     
    int getFormatType();
    String getUrl();
    String getMimeType();
    String getITag();
    boolean isDrc();

     
    String getClen();
    String getBitrate();
    String getProjectionType();
    String getXtags();
    int getWidth();
    int getHeight();
    String getIndex();
    String getInit();
    String getFps();
    String getLmt();
    String getQualityLabel();
    String getFormat();
    boolean isOtf();
    String getOtfInitUrl();
    String getOtfTemplateUrl();
    String getLanguage();
     
    int getTargetDurationSec();
    int getMaxDvrDurationSec();
    int getApproxDurationMs();

     
    String getQuality();
    String getSignature();
    String getAudioSamplingRate();
    String getSourceUrl();
    List<String> getSegmentUrlList();
    List<String> getGlobalSegmentList();
}
