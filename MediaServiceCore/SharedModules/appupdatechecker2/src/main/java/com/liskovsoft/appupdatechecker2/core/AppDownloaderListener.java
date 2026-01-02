package com.liskovsoft.appupdatechecker2.core;

public interface AppDownloaderListener {
    void onApkDownloaded(String path);
    void onDownloadError(Exception e);
}
