package com.liskovsoft.appupdatechecker2.other;

import android.content.Context;
import android.content.SharedPreferences;
import com.liskovsoft.sharedutils.mylogger.Log;
import edu.mit.mobile.android.appupdater.R;

public class SettingsManager {
    public static final long CHECK_INTERVAL_DEFAULT_MS = 60 * 1_000;
    private static final String TAG = SettingsManager.class.getSimpleName();
    private static final String SHARED_PREFERENCES_NAME = "com.liskovsoft.appupdatechecker2.preferences";
    private static final String PREF_CHECK_INTERVAL_MS = "check_interval_ms";
    private static final String PREF_LAST_CHECKED_MS = "last_checked_ms";
    private static final String PREF_APK_PATH = "apk_path";
    private static final String PREF_LATEST_VERSION_NAME = "latest_version_name";
    private static final String PREF_LATEST_VERSION_NUMBER = "latest_version_number";
    private static final String PREF_MAIN_HOST = "main_host";
    private final Context mContext;
    private final SharedPreferences mPrefs;

    public SettingsManager(Context context) {
        Log.d(TAG, "Starting...");

        mContext = context.getApplicationContext();

        mPrefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);

        // defaults are kept in the preference file for ease of tweaking
        //android.preference.PreferenceManager.setDefaultValues(context, SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE, R.xml.upd_prefs, true);
    }

    public long getLastCheckedMs() {
        // Do not store as var cause object not persistent in memory!
        return mPrefs.getLong(PREF_LAST_CHECKED_MS, 0);
    }

    public void setLastCheckedMs(long milliseconds) {
        mPrefs.edit().putLong(PREF_LAST_CHECKED_MS, milliseconds).apply();
    }

    public String getApkPath() {
        return mPrefs.getString(PREF_APK_PATH, null);
    }

    public void setApkPath(String path) {
        mPrefs.edit().putString(PREF_APK_PATH, path).apply();
    }

    public String getLatestVersionName() {
        return mPrefs.getString(PREF_LATEST_VERSION_NAME, null);
    }

    public void setLatestVersionName(String lastVersionName) {
        mPrefs.edit().putString(PREF_LATEST_VERSION_NAME, lastVersionName).apply();
    }

    public int getLatestVersionNumber() {
        return mPrefs.getInt(PREF_LATEST_VERSION_NUMBER, 0);
    }

    public void setLatestVersionNumber(int latestVersionNumber) {
        mPrefs.edit().putInt(PREF_LATEST_VERSION_NUMBER, latestVersionNumber).apply();
    }

    public long getMinIntervalMs() {
        String interval = mPrefs.getString(PREF_CHECK_INTERVAL_MS, null);
        return interval != null ? Long.parseLong(interval) : CHECK_INTERVAL_DEFAULT_MS;
    }

    public void setMinIntervalMs(long milliseconds) {
        mPrefs.edit().putString(PREF_CHECK_INTERVAL_MS, String.valueOf(milliseconds)).apply();
    }

    public void setPreferredHost(String host) {
        mPrefs.edit().putString(PREF_MAIN_HOST, host).apply();
    }

    public String getPreferredHost() {
        return mPrefs.getString(PREF_MAIN_HOST, null);
    }
}
