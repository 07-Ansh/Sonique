package com.liskovsoft.youtubeapi.search.models;

import com.liskovsoft.youtubeapi.common.models.items.ItemWrapper;
import com.liskovsoft.googlecommon.common.converters.jsonpath.JsonPath;

import java.util.List;

public class SearchResultContinuation {
    @JsonPath({
             
            "$.continuationContents.sectionListContinuation.contents[0].shelfRenderer.content.horizontalListRenderer.items[*]",  
            "$.continuationContents.horizontalListContinuation.items[*]",  
            "$.continuationContents.itemSectionContinuation.contents[*]",  

             
            "$.continuationContents.sectionListContinuation.contents[0].itemSectionRenderer.contents[*]"  
    })
    private List<ItemWrapper> mItemWrappers;

    @JsonPath({
            "$.continuationContents.sectionListContinuation.contents[0].shelfRenderer.content.horizontalListRenderer.continuations[0].nextContinuationData.continuation",  
            "$.continuationContents.horizontalListContinuation.continuations[0].nextContinuationData.continuation",  
            "$.continuationContents.sectionListContinuation.contents[0].itemSectionRenderer.continuations[0].nextContinuationData.continuation",
            "$.continuationContents.itemSectionContinuation.continuations[0].nextContinuationData.continuation"
    })
    private String mNextPageKey;

     
    @JsonPath("$.estimatedResults")
    private String mEstimatedResults;

    public List<ItemWrapper> getItemWrappers() {
        return mItemWrappers;
    }

    public String getNextPageKey() {
        return mNextPageKey;
    }

    public String getEstimatedResults() {
        return mEstimatedResults;
    }
}
