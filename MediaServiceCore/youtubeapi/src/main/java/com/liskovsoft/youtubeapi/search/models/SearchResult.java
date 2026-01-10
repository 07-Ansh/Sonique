package com.liskovsoft.youtubeapi.search.models;

import com.liskovsoft.googlecommon.common.converters.jsonpath.JsonPath;
import com.liskovsoft.youtubeapi.common.models.items.ItemWrapper;

import java.util.List;

public class SearchResult {
     
     
     
     
     
     
     
     
     
     

     
    @JsonPath("$.contents.sectionListRenderer.contents[*]")
    private List<SearchSection> mSections;

     
    @JsonPath("$.estimatedResults")
    private String mEstimatedResults;

    public String getNextPageKey() {
        return mSections != null && mSections.size() > 0 ? mSections.get(0).getNextPageKey() : null;
    }

    public List<ItemWrapper> getItemWrappers() {
        return mSections != null && mSections.size() > 0 ? mSections.get(0).getItemWrappers() : null;
    }

    public List<SearchSection> getSections() {
        return mSections;
    }

    public String getEstimatedResults() {
        return mEstimatedResults;
    }
}
