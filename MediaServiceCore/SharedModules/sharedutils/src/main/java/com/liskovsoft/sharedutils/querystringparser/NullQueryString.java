package com.liskovsoft.sharedutils.querystringparser;

import androidx.annotation.NonNull;

class NullQueryString implements UrlQueryString {
    private final String mUrl;

    private NullQueryString(String url) {
        mUrl = url;
    }

    public static UrlQueryString parse(String url) {
        return new NullQueryString(url);
    }

    @Override
    public void remove(String key) {
        
    }

    @Override
    public String get(String key) {
        return null;
    }

    @Override
    public float getFloat(String key) {
        return 0;
    }

    @Override
    public void set(String key, String value) {

    }

    @Override
    public void set(String key, int value) {

    }

    @Override
    public void set(String key, float value) {
        
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @NonNull
    @Override
    public String toString() {
        return mUrl;
    }

    @Override
    public boolean contains(String key) {
        return false;
    }
}
