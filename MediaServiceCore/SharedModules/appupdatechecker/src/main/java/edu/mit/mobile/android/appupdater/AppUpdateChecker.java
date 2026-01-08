package edu.mit.mobile.android.appupdater;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.locale.LocaleUtility;
import com.liskovsoft.sharedutils.mylogger.Log;
import edu.mit.mobile.android.appupdater.downloadmanager.MyDownloadManager;
import edu.mit.mobile.android.appupdater.downloadmanager.MyDownloadManager.MyRequest;
import edu.mit.mobile.android.utils.StreamUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

 
public class AppUpdateChecker {
    private final static String TAG = AppUpdateChecker.class.getSimpleName();

    public static final String SHARED_PREFERENCES_NAME = "edu.mit.mobile.android.appupdater.preferences";
    public static final String PREF_ENABLED = "enabled", PREF_MIN_INTERVAL = "min_interval", PREF_LAST_UPDATED = "last_checked";

    private int currentAppVersion;

    private JSONObject pkgInfo;
    private final Context mContext;

    private final OnAppUpdateListener mUpdateListener;
    private SharedPreferences mPrefs;

    private static final int MILLISECONDS_IN_MINUTE = 60000;
    private boolean mInProgress;
    
    public AppUpdateChecker(Context context, OnAppUpdateListener updateListener) {
        mContext = context;
        mUpdateListener = updateListener;

        try {
            currentAppVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (final NameNotFoundException e) {
            Log.e(TAG, "Cannot get version for self! Who am I?! What's going on!? I'm so confused :-(");
            return;
        }

        mPrefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
         
         
        PreferenceManager.setDefaultValues(context, SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE, R.xml.preferences, true);
    }

     

    public int getMinInterval() {
        return Integer.parseInt(mPrefs.getString(PREF_MIN_INTERVAL, "60"));
    }

    public void setMinInterval(int minutes) {
        mPrefs.edit().putString(PREF_MIN_INTERVAL, String.valueOf(minutes)).apply();
    }

    public boolean getEnabled() {
        return mPrefs.getBoolean(PREF_ENABLED, true);
    }

    public void setEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(PREF_ENABLED, enabled).apply();
    }

     
    public boolean isStale() {
        return System.currentTimeMillis() - mPrefs.getLong(PREF_LAST_UPDATED, 0) > getMinInterval() * MILLISECONDS_IN_MINUTE;
    }

     
    public void checkForUpdates(String[] versionListUrls) {
        if (getEnabled() && isStale()) {
            forceCheckForUpdates(versionListUrls);
        }
    }

     
    public void forceCheckForUpdatesIfEnabled(String[] versionListUrls) {
        if (getEnabled()) {
            forceCheckForUpdates(versionListUrls);
        }
    }

     
    public void forceCheckForUpdatesIfStalled(String[] versionListUrls) {
        if (isStale()) {
            forceCheckForUpdates(versionListUrls);
        }
    }

     
    public void forceCheckForUpdates(String[] versionListUrls) {
        Log.d(TAG, "checking for updates...");

        if (mInProgress || mUpdateListener.tryInstallPendingUpdate()) {
            return;
        }

        if (versionListUrls == null || versionListUrls.length == 0) {
            Log.w(TAG, "Supplied url update list is null or empty");
        } else if (mJsonUpdateTask == null) {
            mJsonUpdateTask = new GetVersionJsonTask();
            mJsonUpdateTask.execute(versionListUrls);
        } else {
            Log.w(TAG, "checkForUpdates() called while already checking for updates. Ignoring...");
        }
    }

     
    @SuppressWarnings("unchecked")
    private void triggerFromJson(JSONObject jo) throws JSONException {

        final ArrayList<String> changelog = new ArrayList<String>();

         
         
        final TreeMap<Integer, JSONObject> versionMap = new TreeMap<Integer, JSONObject>(new Comparator<Integer>() {
            public int compare(Integer object1, Integer object2) {
                return object2.compareTo(object1);
            }
        });

        for (final Iterator<String> i = jo.keys(); i.hasNext(); ) {
            final String versionName = i.next();
            if (versionName.equals("package")) {
                pkgInfo = jo.getJSONObject(versionName);
                continue;
            }
            final JSONObject versionInfo = jo.getJSONObject(versionName);
            versionInfo.put("versionName", versionName);

            final int versionCode = versionInfo.getInt("versionCode");
            versionMap.put(versionCode, versionInfo);
        }
        final int latestVersionNumber = versionMap.firstKey();
        final String latestVersionName = versionMap.get(latestVersionNumber).getString("versionName");

        final Uri[] downloadUrls;

        if (pkgInfo.has("downloadUrlList")) {
            JSONArray urls = pkgInfo.getJSONArray("downloadUrlList");
            downloadUrls = parse(urls);
        } else {
            String url = pkgInfo.getString("downloadUrl");
            downloadUrls = new Uri[]{Uri.parse(url)};
        }

        if (currentAppVersion > latestVersionNumber) {
            Log.d(TAG, "We're newer than the latest published version (" + latestVersionName + "). Living in the future...");
            mUpdateListener.appUpdateStatus(true, latestVersionName, null, downloadUrls);
            return;
        }

        if (currentAppVersion == latestVersionNumber) {
            Log.d(TAG, "We're at the latest version (" + currentAppVersion + ")");
            mUpdateListener.appUpdateStatus(true, latestVersionName, null, downloadUrls);
            return;
        }

         
        for (final Entry<Integer, JSONObject> version : versionMap.headMap(currentAppVersion).entrySet()) {
            final JSONObject versionInfo = version.getValue();

            JSONArray versionChangelog = versionInfo.optJSONArray("changelog_" + LocaleUtility.getCurrentLanguage(mContext));

            if (versionChangelog == null) {
                versionChangelog = versionInfo.optJSONArray("changelog");
            }

            if (versionChangelog != null) {
                final int len = versionChangelog.length();
                for (int i = 0; i < len; i++) {
                    changelog.add(versionChangelog.getString(i));
                }
            }
        }

        mUpdateListener.appUpdateStatus(false, latestVersionName, changelog, downloadUrls);
    }

    private Uri[] parse(JSONArray urls) {
        List<Uri> res = new ArrayList<>();
        for (int i = 0; i < urls.length(); i++) {
            String url = null;
            try {
                url = urls.getString(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (url != null)
                res.add(Uri.parse(url));
        }
        return res.toArray(new Uri[] {});
    }

    private class VersionCheckException extends Exception {
         
        private static final long serialVersionUID = 397593559982487816L;

        public VersionCheckException(String msg) {
            super(msg);
        }
    }

     
    public void startUpgrade() {
        try {
            final Uri downloadUri = Uri.parse(pkgInfo.getString("downloadUrl"));
            mContext.startActivity(new Intent(Intent.ACTION_VIEW, downloadUri));
        } catch (final JSONException e) {
            e.printStackTrace();
        }
    }

    private GetVersionJsonTask mJsonUpdateTask;

    private class GetVersionJsonTask extends AsyncTask<String[], Integer, JSONObject> {
        private String errorMsg = null;

        @Override
        protected void onProgressUpdate(Integer... values) {
            Log.d(TAG, "update check progress: " + values[0]);
            super.onProgressUpdate(values);
        }

        @Override
        protected JSONObject doInBackground(String[]... params) {
            mInProgress = true;
            publishProgress(0);

            final String[] urls = params[0];
            JSONObject jo = null;

            publishProgress(50);

            for (String url : urls) {
                jo = getJSON(url);
                if (jo != null)
                    break;
            }

            return jo;
        }

        private JSONObject getJSON(String urlStr) {
            JSONObject jo = null;
            try {
                MyDownloadManager manager = new MyDownloadManager(mContext);
                MyRequest request = new MyRequest(Uri.parse(urlStr));
                long reqId = manager.enqueue(request);

                InputStream content = manager.getStreamForDownloadedFile(reqId);
                jo = new JSONObject(StreamUtils.inputStreamToString(content));

                 
                mPrefs.edit().putLong(PREF_LAST_UPDATED, System.currentTimeMillis()).apply();
            } catch (final IllegalStateException | JSONException ex) {
                Log.e(TAG, ex.getMessage(), ex);
                errorMsg = Helpers.toString(ex);
            } catch (final Exception ex) {
                throw new IllegalStateException(ex);
            } finally {
                publishProgress(100);
            }

            return jo;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (result == null) {
                Log.e(TAG, errorMsg);
            } else {
                try {
                    triggerFromJson(result);
                } catch (final JSONException e) {
                    Log.e(TAG, "Error in JSON version file.", e);
                }
            }

            mInProgress = false;
        }
    }

    public boolean cancelPendingUpdate() {
        boolean mCancelInstall = mUpdateListener.cancelPendingUpdate();
        return mInProgress || mCancelInstall;
    }
}
