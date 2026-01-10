package com.liskovsoft.youtubeapi.browse.v1.models.grid;

import com.liskovsoft.googlecommon.common.converters.jsonpath.JsonPath;
import com.liskovsoft.youtubeapi.common.models.items.ItemWrapper;

import java.util.Collections;
import java.util.List;

 
public class GridTabContinuation {
    @JsonPath({"$.continuationContents.gridContinuation.items[*]",                                    
               "$.continuationContents.tvSurfaceContentContinuation.content.gridRenderer.items[*]",  
               "$.continuationContents.sectionListContinuation.contents[0].itemSectionRenderer.contents[*]"  
    })
    private List<ItemWrapper> mItemWrappers;

    @JsonPath({"$.continuationContents.gridContinuation.continuations[0].nextContinuationData.continuation",               
               "$.continuationContents.tvSurfaceContentContinuation.content.gridRenderer.continuations[0].nextContinuationData.continuation",  
               "$.continuationContents.sectionListContinuation.continuations[0].nextContinuationData.continuation"  
    })
    private String mNextPageKey;

     
    @JsonPath("$.continuationContents.tvSurfaceContentContinuation.header.tvSurfaceHeaderRenderer.buttons[*].buttonRenderer")
    private List<ChannelButton> mChannelButtons;

     
    @JsonPath("$.continuationContents.tvSurfaceContentContinuation.content.genericPromoRenderer.actionButton.buttonRenderer")
    private ChannelButton mEmptyChannelButton;

     
    @JsonPath("$.responseContext.visitorData")
    private String mVisitorData;

    public String getNextPageKey() {
        return mNextPageKey;
    }

    public void setNextPageKey(String nextPageKey) {
        mNextPageKey = nextPageKey;
    }

    public List<ItemWrapper> getItemWrappers() {
        return mItemWrappers;
    }

    public void setItemWrappers(List<ItemWrapper> itemWrappers) {
        mItemWrappers = itemWrappers;
    }

    public List<ChannelButton> getChannelButtons() {
        if (mChannelButtons != null) {
            return mChannelButtons;
        }

         
        if (mEmptyChannelButton != null) {
            return Collections.singletonList(mEmptyChannelButton);
        }

        return null;
    }

     
    public String getBrowseId() {
        if (getChannelButtons() != null) {
            for (ChannelButton button : getChannelButtons()) {
                if (button.getBrowseId() != null) {
                    return button.getBrowseId();
                }
            }
        }

        return null;
    }

     
    public String getParams() {
        if (getChannelButtons() != null) {
            for (ChannelButton button : getChannelButtons()) {
                if (button.getParams() != null) {
                    return button.getParams();
                }
            }
        }

        return null;
    }

     
    public String getCanonicalBaseUrl() {
        if (getChannelButtons() != null) {
            for (ChannelButton button : getChannelButtons()) {
                if (button.getCanonicalBaseUrl() != null) {
                    return button.getCanonicalBaseUrl();
                }
            }
        }

        return null;
    }

    public String getVisitorData() {
        return mVisitorData;
    }
}
