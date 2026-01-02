package com.liskovsoft.sharedutils.querystringparser;

import android.net.Uri;
import com.liskovsoft.sharedutils.helpers.Helpers;

import java.io.InputStream;

public class UrlQueryStringFactory {
    public static UrlQueryString parse(Uri url) {
        if (url == null) {
            return null;
        }

        return parse(url.toString());
    }

    public static UrlQueryString parse(InputStream urlContent) {
        return parse(Helpers.toString(urlContent));
    }

    //public static UrlQueryString parse(String url) {
    //    UrlQueryString pathQueryString = PathQueryString.parse(url);
    //
    //    if (pathQueryString.isValid()) {
    //        return pathQueryString;
    //    }
    //
    //    UrlQueryString urlQueryString = UrlEncodedQueryString.parse(url);
    //
    //    if (urlQueryString.isValid()) {
    //        return urlQueryString;
    //    }
    //
    //    return NullQueryString.parse(url);
    //}

    public static UrlQueryString parse(String url) {
        return CombinedQueryString.parse(url);
    }
}
