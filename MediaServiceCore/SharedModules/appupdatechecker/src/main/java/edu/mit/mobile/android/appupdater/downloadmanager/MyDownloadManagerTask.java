package edu.mit.mobile.android.appupdater.downloadmanager;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import com.liskovsoft.sharedutils.helpers.FileHelpers;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.sharedutils.mylogger.Log;

import java.io.File;

public class MyDownloadManagerTask extends AsyncTask<Void, Integer, Integer> {
    private static final String TAG = MyDownloadManagerTask.class.getSimpleName();
    private static final String DEFAULT_DOWNLOAD_FILE_NAME = "tmp.apk";

     
    public static abstract class DownloadListener {
         
        public void onDownloadStarted() {}

         
        public void onDownloadUpdated(int percentage) {}

         
        public void onDownloadCancelled() {}

         
        public void onDownloadCompleted(Uri uri) {}

         
        public void onDownloadFailed(int status, int error) {}
    }

    private DownloadListener mListener;
    private Context mContext;
    private String mDownloadUrl;
    private MyDownloadManager mDownloadManager;
    private long mDownloadId;
    private boolean isDone;

    public MyDownloadManagerTask(DownloadListener listener, Context context, String url) {
        super();
        mListener = listener;
        mContext = context;
        mDownloadUrl = url;
        mDownloadManager = new MyDownloadManager(mContext);
    }

    @Override
    protected void onPreExecute() {
        Log.d(TAG, "DownloadManagerTask started, " + mDownloadUrl);
        mListener.onDownloadStarted();
    }

    private void showMessage(final String msg) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show());
    }

    @Override
    protected Integer doInBackground(Void... params) {
        if (mDownloadUrl == null) return DownloadManager.STATUS_FAILED;


        String savedFile = DEFAULT_DOWNLOAD_FILE_NAME;

        File downloadDir = FileHelpers.getCacheDir(mContext);

        File downloadFile = new File(downloadDir, savedFile);
        if (downloadFile.isFile()) downloadFile.delete();

        MyDownloadManager.MyRequest request = new MyDownloadManager.MyRequest(Uri.parse(mDownloadUrl));
        request.setDestinationUri(Uri.fromFile(downloadFile));
        request.setProgressListener((bytesRead, contentLength, done) -> {
            publishProgress((int)bytesRead, (int)contentLength);
            isDone = done;
        });

        try {
            mDownloadId = mDownloadManager.enqueue(request);
        } catch (IllegalStateException ex) {
            Log.e(TAG, ex.getMessage(), ex);
            MessageHelpers.showMessage(mContext, TAG, ex);
        }

        return isDone ? DownloadManager.STATUS_SUCCESSFUL : DownloadManager.STATUS_FAILED;
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

        if (result == DownloadManager.STATUS_SUCCESSFUL) {
            try {
                Uri uri = mDownloadManager.getUriForDownloadedFile(mDownloadId);

                Log.d(TAG, "Uri for downloaded file: " + uri);

                if (uri != null) {
                    mListener.onDownloadCompleted(uri);
                }
            } catch (IllegalStateException ex) {
                Log.e(TAG, ex.getMessage(), ex);
                MessageHelpers.showMessage(mContext, TAG, ex);
                mListener.onDownloadFailed(result, DownloadManager.ERROR_UNKNOWN);
            }
        } else {
            int error = DownloadManager.ERROR_UNKNOWN;
            mListener.onDownloadFailed(result, error);
        }
    }
}
