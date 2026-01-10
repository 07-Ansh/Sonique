package com.liskovsoft.appupdatechecker2;

import java.util.List;

public interface AppUpdateCheckerListener {
    String UPDATE_CHECK_DISABLED = "Update check disabled";
    String LATEST_VERSION = "Latest version";
     
    void onUpdateFound(String versionName, List<String> changelog, String apkPath);
    void onUpdateError(Exception error);
}
