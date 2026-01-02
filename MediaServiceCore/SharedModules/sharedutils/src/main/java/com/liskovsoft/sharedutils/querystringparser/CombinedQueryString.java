package com.liskovsoft.sharedutils.querystringparser;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

class CombinedQueryString implements UrlQueryString {
    private final List<UrlQueryString> mQueryStrings = new ArrayList<>();

    public CombinedQueryString(String url) {
        UrlQueryString urlQueryString = UrlEncodedQueryString.parse(url);

        if (urlQueryString.isValid()) {
            mQueryStrings.add(urlQueryString);
        }

        UrlQueryString pathQueryString = PathQueryString.parse(url);

        if (pathQueryString.isValid()) {
            mQueryStrings.add(pathQueryString);
        }

        if (mQueryStrings.isEmpty()) {
            mQueryStrings.add(NullQueryString.parse(url));
        }
    }

    public static UrlQueryString parse(String url) {
        return new CombinedQueryString(url);
    }

    @Override
    public void remove(String key) {
        for (UrlQueryString queryString : mQueryStrings) {
            queryString.remove(key);
        }
    }

    @Override
    public String get(String key) {
        for (UrlQueryString queryString : mQueryStrings) {
            String value = queryString.get(key);
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    @Override
    public float getFloat(String key) {
        for (UrlQueryString queryString : mQueryStrings) {
            float value = queryString.getFloat(key);
            if (value != 0) {
                return value;
            }
        }

        return 0;
    }

    @Override
    public void set(String key, String value) {
        for (UrlQueryString queryString : mQueryStrings) {
            queryString.set(key, value);
        }
    }

    @Override
    public void set(String key, int value) {
        for (UrlQueryString queryString : mQueryStrings) {
            queryString.set(key, value);
        }
    }

    @Override
    public void set(String key, float value) {
        for (UrlQueryString queryString : mQueryStrings) {
            queryString.set(key, value);
        }
    }

    @Override
    public boolean isEmpty() {
        for (UrlQueryString queryString : mQueryStrings) {
            return queryString.isEmpty();
        }

        return true;
    }

    @Override
    public boolean isValid() {
        for (UrlQueryString queryString : mQueryStrings) {
            return queryString.isValid();
        }

        return false;
    }

    @Override
    public boolean contains(String key) {
        for (UrlQueryString queryString : mQueryStrings) {
            boolean contains = queryString.contains(key);
            if (contains) {
                return true;
            }
        }

        return false;
    }

    @NonNull
    @Override
    public String toString() {
        for (UrlQueryString queryString : mQueryStrings) {
            return queryString.toString();
        }

        return super.toString();
    }
}
