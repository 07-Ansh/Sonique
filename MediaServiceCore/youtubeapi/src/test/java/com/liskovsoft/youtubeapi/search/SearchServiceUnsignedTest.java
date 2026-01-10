package com.liskovsoft.youtubeapi.search;

import com.liskovsoft.youtubeapi.search.models.SearchResult;
import com.liskovsoft.youtubeapi.search.models.SearchResultContinuation;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class SearchServiceUnsignedTest {
    private SearchService mService;

    @Before
    public void setUp() {
         
         
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");

        ShadowLog.stream = System.out;  

        mService = new SearchService();
    }

    @Test
    public void testSearchNotEmpty() {
        SearchResult searchResult = mService.getSearch("any search text");
        assertTrue("search not empty?", searchResult.getItemWrappers().size() != 0);

        SearchResultContinuation nextSearchResult = mService.continueSearch(searchResult.getNextPageKey());
        assertTrue("next search not empty?", nextSearchResult.getItemWrappers().size() != 0);
    }
}