package com.liskovsoft.appupdatechecker2.core;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;

import com.liskovsoft.appupdatechecker2.other.downloadmanager.DownloadManager;
import com.liskovsoft.appupdatechecker2.other.downloadmanager.DownloadManager.MyRequest;
import com.liskovsoft.sharedutils.helpers.DeviceHelpers;
import com.liskovsoft.sharedutils.locale.LocaleUtility;
import com.liskovsoft.sharedutils.mylogger.Log;

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

import edu.mit.mobile.android.utils.StreamUtils;

 
public class AppVersionChecker {
    private final static String TAG = AppVersionChecker.class.getSimpleName();
    private int mCurrentAppVersion;
    private JSONObject mVersionInfo;
    private final Context mContext;
    private boolean mInProgress;
    private final AppVersionCheckerListener mListener;

    @SuppressWarnings("deprecation")
    public AppVersionChecker(Context context, AppVersionCheckerListener listener) {
        mContext = context;
        mListener = listener;

        try {
            mCurrentAppVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (final NameNotFoundException e) {
            String msg = "Cannot get version for self!";
            Log.e(TAG, msg);
            mListener.onCheckError(new IllegalStateException(msg));
        }
    }

     
    public void checkForUpdates(Uri[] versionListUrls) {
        Log.d(TAG, "Checking for updates...");

        if (mInProgress) {
            Log.e(TAG, "Another update is running. Cancelling...");
            return;
        }

        if (versionListUrls == null || versionListUrls.length == 0) {
            Log.w(TAG, "Supplied url update list is null or empty");
        } else if (mJsonUpdateTask == null) {
            mListener.processDownloadUrls(versionListUrls);

            mJsonUpdateTask = new GetVersionJsonTask();
            mJsonUpdateTask.execute(versionListUrls);
        } else {
            String msg = "checkForUpdates() called while already checking for updates. Ignoring...";
            Log.e(TAG, msg);
            mListener.onCheckError(new IllegalStateException(msg));
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
                mVersionInfo = jo.getJSONObject(versionName);
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

        if (mVersionInfo.has("downloadUrlList_" + DeviceHelpers.getPrimaryAbi())) {
            JSONArray urls = mVersionInfo.getJSONArray("downloadUrlList_" + DeviceHelpers.getPrimaryAbi());
            downloadUrls = parse(urls);
        } else if (mVersionInfo.has("downloadUrlList")) {
            JSONArray urls = mVersionInfo.getJSONArray("downloadUrlList");
            downloadUrls = parse(urls);
        } else {
            String url = mVersionInfo.getString("downloadUrl");
            downloadUrls = new Uri[]{Uri.parse(url)};
        }

        if (downloadUrls != null) {
            mListener.processDownloadUrls(downloadUrls);
        }

        if (mCurrentAppVersion > latestVersionNumber) {
            Log.d(TAG, "We're newer than the latest published version (" + latestVersionName + "). Living in the future...");
            mListener.onChangelogReceived(true, latestVersionName, latestVersionNumber, null, downloadUrls);
            return;
        }

        if (mCurrentAppVersion == latestVersionNumber) {
            Log.d(TAG, "We're at the latest version (" + mCurrentAppVersion + ")");
            mListener.onChangelogReceived(true, latestVersionName, latestVersionNumber, null, downloadUrls);
            return;
        }

         
        for (final Entry<Integer, JSONObject> version : versionMap.headMap(mCurrentAppVersion).entrySet()) {
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

        mListener.onChangelogReceived(false, latestVersionName, latestVersionNumber, changelog, downloadUrls);
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
            final Uri downloadUri = Uri.parse(mVersionInfo.getString("downloadUrl"));
            mContext.startActivity(new Intent(Intent.ACTION_VIEW, downloadUri));
        } catch (final JSONException e) {
            e.printStackTrace();
        }
    }

    private GetVersionJsonTask mJsonUpdateTask;

    private class GetVersionJsonTask extends AsyncTask<Uri[], Integer, JSONObject> {
        private Exception mLastException;

        @Override
        protected void onProgressUpdate(Integer... values) {
            Log.d(TAG, "update check progress: " + values[0]);
            super.onProgressUpdate(values);
        }

        @Override
        protected JSONObject doInBackground(Uri[]... params) {
            mInProgress = true;
            publishProgress(0);

            final Uri[] urls = params[0];
            JSONObject jo = null;

            publishProgress(50);

            for (Uri url : urls) {
                jo = getJSON(url);
                if (jo != null)
                    break;
            }

            return jo;
        }

        private JSONObject getJSON(Uri urlStr) {
            JSONObject jo = null;
            try {
                DownloadManager manager = new DownloadManager(mContext);
                MyRequest request = new MyRequest(urlStr);
                long reqId = manager.enqueue(request);

                InputStream content = manager.getStreamForDownloadedFile(reqId);
                jo = new JSONObject(StreamUtils.inputStreamToString(content));
            } catch (final Exception ex) {
                 
                 
                Log.e(TAG, ex.getMessage(), ex);
                mLastException = ex;
            } finally {
                publishProgress(100);
            }

            return jo;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (result != null) {
                try {
                    triggerFromJson(result);
                } catch (final JSONException e) {
                    String msg = "Error in JSON version file.";
                    Log.e(TAG, msg, e);
                    mListener.onCheckError(new IllegalStateException(msg));
                }
            } else {
                mListener.onCheckError(mLastException != null ? mLastException : new Exception("Unknown error. JSON content is null"));
            }

            mInProgress = false;
            mJsonUpdateTask = null;
        }
    }

    public boolean isInProgress() {
        return mInProgress;
    }
}
