package com.liskovsoft.appupdatechecker2.other.downloadmanager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import com.liskovsoft.sharedutils.helpers.FileHelpers;
import com.liskovsoft.sharedutils.mylogger.Log;

import java.io.File;

public class DownloadManagerTask extends AsyncTask<Void, Integer, Integer> {
    private static final String TAG = DownloadManagerTask.class.getSimpleName();
    private static final String DEFAULT_DOWNLOAD_FILE_NAME = "tmp.apk";

     
    public static abstract class DownloadListener {
         
        public void onDownloadStarted() {}

         
        public void onDownloadUpdated(int percentage) {}

         
        public void onDownloadCancelled() {}

         
        public void onDownloadCompleted(Uri uri) {}

         
        public void onDownloadFailed(int status, int error) {}
    }

    private final DownloadListener mListener;
    @SuppressLint("StaticFieldLeak")
    private final Context mContext;
    private final String mDownloadUrl;
    private final DownloadManager mDownloadManager;
    private long mDownloadId;
    private boolean isDone;

    public DownloadManagerTask(DownloadListener listener, Context context, String url) {
        super();
        mListener = listener;
        mContext = context;
        mDownloadUrl = url;
        mDownloadManager = new DownloadManager(mContext);
    }

    @Override
    protected void onPreExecute() {
        Log.d(TAG, "DownloadManagerTask started, " + mDownloadUrl);
        mListener.onDownloadStarted();
    }

    @Override
    protected Integer doInBackground(Void... params) {
        if (mDownloadUrl == null) return android.app.DownloadManager.STATUS_FAILED;


        String savedFile = DEFAULT_DOWNLOAD_FILE_NAME;

        File downloadDir = FileHelpers.getCacheDir(mContext);

        File downloadFile = new File(downloadDir, savedFile);
        if (downloadFile.isFile()) downloadFile.delete();

        DownloadManager.MyRequest request = new DownloadManager.MyRequest(Uri.parse(mDownloadUrl));
        request.setDestinationUri(Uri.fromFile(downloadFile));
        request.setProgressListener((bytesRead, contentLength, done) -> {
            publishProgress((int)bytesRead, (int)contentLength);
            isDone = done;
        });

        try {
            mDownloadId = mDownloadManager.enqueue(request);
        } catch (IllegalStateException ex) {
            Log.e(TAG, ex.getMessage(), ex);
             
        }

        return isDone ? android.app.DownloadManager.STATUS_SUCCESSFUL : android.app.DownloadManager.STATUS_FAILED;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        Log.d(TAG, "DownloadManagerTask updated: " + progress[0] + "/" + progress[1]);
        int percentage = 0;
        if (progress[1] > 0) {
            percentage = (int) (progress[0] * 100.0 / progress[1]);
            mListener.onDownloadUpdated(percentage);
        }
    }

    @Override
    protected void onCancelled(Integer result) {
        mDownloadManager.remove(mDownloadId);

        Log.d(TAG, "DownloadManagerTask cancelled");
        mListener.onDownloadCancelled();
    }

    @Override
    protected void onPostExecute(Integer result) {
        Log.d(TAG, "DownloadManagerTask finished, " + result);

        if (result == android.app.DownloadManager.STATUS_SUCCESSFUL) {
            try {
                Uri uri = mDownloadManager.getUriForDownloadedFile(mDownloadId);

                Log.d(TAG, "Uri for downloaded file: " + uri);

                if (uri != null) {
                    mListener.onDownloadCompleted(uri);
                }
            } catch (IllegalStateException ex) {
                Log.e(TAG, ex.getMessage(), ex);
                 
                mListener.onDownloadFailed(result, android.app.DownloadManager.ERROR_UNKNOWN);
            }
        } else {
            int error = android.app.DownloadManager.ERROR_UNKNOWN;
            mListener.onDownloadFailed(result, error);
        }
    }
}
