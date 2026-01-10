package com.liskovsoft.youtubeapi.browse.v1.models.sections;

import com.liskovsoft.googlecommon.common.converters.jsonpath.JsonPath;

import java.util.ArrayList;
import java.util.List;

public class SectionTabList {
    @JsonPath("$.contents.tvBrowseRenderer.content.tvSecondaryNavRenderer.sections[0].tvSecondaryNavSectionRenderer.tabs[*].tabRenderer")
    private List<SectionTab> mTabs;

    @JsonPath("$.contents.tvBrowseRenderer")
    private SectionTab mSingleTab;

    @JsonPath("$.responseContext.visitorData")
    private String mVisitorData;

     
    public List<SectionTab> getTabs() {
        if (mTabs == null && mSingleTab != null) {
            mTabs = new ArrayList<>();
            mTabs.add(mSingleTab);
        }

        return mTabs;
    }

    public String getVisitorData() {
        return mVisitorData;
    }
}
