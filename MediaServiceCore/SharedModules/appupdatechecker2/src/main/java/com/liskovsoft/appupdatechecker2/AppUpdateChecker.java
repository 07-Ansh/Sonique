package com.liskovsoft.appupdatechecker2;

import android.content.Context;
import android.net.Uri;
import com.liskovsoft.appupdatechecker2.core.AppDownloader;
import com.liskovsoft.appupdatechecker2.core.AppDownloaderListener;
import com.liskovsoft.appupdatechecker2.core.AppVersionChecker;
import com.liskovsoft.appupdatechecker2.core.AppVersionCheckerListener;
import com.liskovsoft.appupdatechecker2.other.SettingsManager;
import com.liskovsoft.sharedutils.helpers.FileHelpers;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.mylogger.Log;

import java.util.Arrays;
import java.util.List;

public class AppUpdateChecker implements AppVersionCheckerListener, AppDownloaderListener {
    private static final String TAG = AppUpdateChecker.class.getSimpleName();
    private static final int FRESH_TIME_MS = 15 * 60 * 1_000; // 15 minutes
    private static final int MIN_APK_SIZE_BYTES = 1_000_000; // 1 MB
    private final Context mContext;
    private final AppVersionChecker mVersionChecker;
    private final AppDownloader mDownloader;
    private final AppUpdateCheckerListener mListener;
    private final SettingsManager mSettingsManager;
    private List<String> mChangeLog;
    private String mLatestVersionName;
    private int mLatestVersionNumber;

    public AppUpdateChecker(Context context, AppUpdateCheckerListener listener) {
        this(context, listener, MIN_APK_SIZE_BYTES);
    }

    public AppUpdateChecker(Context context, AppUpdateCheckerListener listener, int minApkSizeBytes) {
        Log.d(TAG, "Starting...");

        FileHelpers.checkCachePermissions(context); // should be an Activity context

        mContext = context.getApplicationContext();
        mListener = listener;
        mVersionChecker = new AppVersionChecker(mContext, this);
        mDownloader = new AppDownloader(mContext, this, minApkSizeBytes);
        mSettingsManager = new SettingsManager(mContext);

        // Cleanup the storage. I don't want to accidentally install old version.
        //FileHelpers.delete(mSettingsManager.getApkPath());
    }

    /**
     * You normally shouldn't need to call this, as {@link #checkForUpdates(String[] versionListUrls)} checks it before doing any updates.
     *
     * @return true if the updater should check for updates
     */
    private boolean isStale() {
        if (mSettingsManager.getMinIntervalMs() < 0) {
            return false;
        }

        return System.currentTimeMillis() - mSettingsManager.getLastCheckedMs() > mSettingsManager.getMinIntervalMs();
    }

    public void checkForUpdates(String updateManifestUrl) {
        checkForUpdates(new String[]{updateManifestUrl});
    }

    /**
     * Checks for updates if updates haven't been checked for recently and if checking is enabled.
     */
    public void checkForUpdates(String[] updateManifestUrls) {
        if (isUpdateCheckEnabled() && isStale()) {
            checkForUpdatesInt(updateManifestUrls);
        } else {
            mListener.onUpdateError(new IllegalStateException(AppUpdateCheckerListener.UPDATE_CHECK_DISABLED));
        }
    }

    public void forceCheckForUpdates(String updateManifestUrl) {
        forceCheckForUpdates(new String[]{updateManifestUrl});
    }

    public void forceCheckForUpdates(String[] updateManifestUrls) {
        checkForUpdatesInt(updateManifestUrls);
    }

    private void checkForUpdatesInt(String[] updateManifestUrls) {
        if (!checkPostponed()) {
            Uri[] uris = new Uri[updateManifestUrls.length];

            for (int i = 0; i < updateManifestUrls.length; i++) {
                uris[i] = Uri.parse(updateManifestUrls[i]);
            }

            checkForUpdatesInt(uris);
        }
    }

    private void checkForUpdatesInt(Uri[] updateManifestUrls) {
        if (!checkPostponed()) {
            mVersionChecker.checkForUpdates(updateManifestUrls);
        }
    }

    private boolean checkPostponed() {
        return false;
    }

    @Override
    public void onChangelogReceived(boolean isLatestVersion, String latestVersionName, int latestVersionNumber, List<String> changelog, Uri[] downloadUris) {
        if (!isLatestVersion) {
            if (downloadUris != null) {
                mChangeLog = changelog;
                mLatestVersionName = latestVersionName;
                mLatestVersionNumber = latestVersionNumber;

                if (latestVersionNumber == mSettingsManager.getLatestVersionNumber() &&
                        checkApk(mSettingsManager.getApkPath())) {
                    mListener.onUpdateFound(latestVersionName, changelog, mSettingsManager.getApkPath());
                } else {
                    mDownloader.download(downloadUris);
                }
            }
        } else {
            // No update is needed.
            mSettingsManager.setLastCheckedMs(System.currentTimeMillis());

            // Remove old apks.
            FileHelpers.delete(mSettingsManager.getApkPath());

            mListener.onUpdateError(new IllegalStateException(AppUpdateCheckerListener.LATEST_VERSION));
        }
    }

    @Override
    public void onApkDownloaded(String path) {
        if (!checkApk(path)) {
            return;
        }

        mSettingsManager.setApkPath(path);
        mSettingsManager.setLatestVersionName(mLatestVersionName);
        mSettingsManager.setLatestVersionNumber(mLatestVersionNumber);

        Log.d(TAG, "App update received. Apk path: " + path);
        Log.d(TAG, "App update received. Changelog: " + mChangeLog);

        mListener.onUpdateFound(mLatestVersionName, mChangeLog, path);
    }

    @Override
    public void onCheckError(Exception e) {
        mListener.onUpdateError(e);
    }

    @Override
    public void onDownloadError(Exception e) {
        mListener.onUpdateError(e);
    }

    @Override
    public void processDownloadUrls(Uri[] downloadUrls) {
        String preferredHost = getPreferredHost();

        if (preferredHost == null) {
            return;
        }

        Arrays.sort(downloadUrls, ((o1, o2) -> {
            boolean firstMatch = o1 != null && Helpers.equals(preferredHost, o1.getHost());
            boolean secondMatch = o2 != null && Helpers.equals(preferredHost, o2.getHost());

            return firstMatch == secondMatch ? 0 : firstMatch ? -1 : 1;
        }));
    }

    public void installUpdate() {
        Helpers.installPackage(mContext, mSettingsManager.getApkPath());
    }

    public void enableUpdateCheck(boolean enable) {
        mSettingsManager.setMinIntervalMs(enable ? SettingsManager.CHECK_INTERVAL_DEFAULT_MS : -1);
    }

    public boolean isUpdateCheckEnabled() {
        return mSettingsManager.getMinIntervalMs() > 0;
    }

    public void setPreferredHost(String host) {
        mSettingsManager.setPreferredHost(host);
    }

    public String getPreferredHost() {
        return mSettingsManager.getPreferredHost();
    }

    private boolean checkApk(String path) {
        // package is not broken
        return FileHelpers.isFreshFile(path, FRESH_TIME_MS) && mContext.getPackageManager().getPackageArchiveInfo(path, 0) != null;
    }
}
