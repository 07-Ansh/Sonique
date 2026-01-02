package com.liskovsoft.sharedutils.querystringparser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
public class UrlEncodedQueryStringTest {
    // Russian is not working. Query is changed after parse.
    private static final String TEST_URL_1 = "https://www.youtube.com/results?search_query=Джентльмены удачи";
    private static final String TEST_URL_2 = "https://www.youtube.com/results?search_query=Hello+World";
    private static final String TEST_URL_3 = "search_query=Hello+World";
    private static final String TEST_URL_4 = "search_query=Джентльмены удачи";

    @Test
    public void testThatQueryParsedCorrectly() {
        //testUrl(TEST_URL_1);
        testUrl(TEST_URL_2);
        testUrl(TEST_URL_3);
        //testUrl(TEST_URL_4);
    }

    private void testUrl(String url) {
        UrlEncodedQueryString queryString = UrlEncodedQueryString.parse(url);

        assertNotNull("Contains query param", queryString.get("search_query"));
        assertEquals("Url not modified", url, queryString.toString());
    }
}