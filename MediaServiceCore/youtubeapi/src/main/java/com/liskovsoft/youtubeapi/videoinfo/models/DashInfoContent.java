package com.liskovsoft.youtubeapi.videoinfo.models;

import com.liskovsoft.googlecommon.common.converters.regexp.RegExp;

public class DashInfoContent extends DashInfoBase {
     
    @RegExp("Sequence-Number: (\\d*)")
    private String mLastSegmentNumStr;

     
    @RegExp("Ingestion-Walltime-Us: (\\d*)")
    private String mLastSegmentTimeUs;

     
     
     

     
    @RegExp("Stream-Duration-Us: (\\d*)")
    private String mStreamDurationUs;

     
     
     

    private int mLastSegmentNum = -1;
    private long mLastSegmentTimeMs = -1;
    private long mStreamDurationMs = -1;

    @Override
    protected int getLastSegmentNum() {
        if (mLastSegmentNum == -1) {
            mLastSegmentNum = Integer.parseInt(mLastSegmentNumStr);
        }

        return mLastSegmentNum;
    }

    @Override
    protected long getLastSegmentTimeMs() {
        if (mLastSegmentTimeMs == -1) {
            mLastSegmentTimeMs = Long.parseLong(mLastSegmentTimeUs) / 1_000;
        }

        return mLastSegmentTimeMs;
    }

    @Override
    protected long getStreamDurationMs() {
        if (mStreamDurationMs == -1) {
            mStreamDurationMs = Long.parseLong(mStreamDurationUs) / 1_000;
        }

        return mStreamDurationMs;
    }
}
