package com.liskovsoft.appupdatechecker2.core;

import android.net.Uri;

import java.util.List;

public interface AppVersionCheckerListener {
	void onChangelogReceived(boolean isLatestVersion, String latestVersionName, int latestVersionNumber, List<String> changelog, Uri[] downloadUris);
	void onCheckError(Exception e);
	void processDownloadUrls(Uri[] downloadUrls);
}