package com.liskovsoft.appupdatechecker2.other.downloadmanager;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import com.liskovsoft.sharedutils.helpers.FileHelpers;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.sharedutils.okhttp.OkHttpManager;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

 
public final class DownloadManager {
    private static final String TAG = DownloadManager.class.getSimpleName();
    private static final int NUM_TRIES = 10;
    private final Context mContext;
    private final OkHttpClient mClient;
    private MyRequest mRequest;
    private long mRequestId;
    private InputStream mResponseStream;
    private int mTotalLen = 0;
    private Uri mFileUri;
    private static final long MAX_DOWN_TIME_MS = 60 * 1_000;  
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36";
    private static final String ACCEPT_CONTENT = "*/*";
    private final Map<String, String> mHeaders = new HashMap<>();

    public DownloadManager(Context context) {
        mContext = context;
        mClient = createOkHttpClient();
        mHeaders.put("User-Agent", USER_AGENT);
        mHeaders.put("Accept", ACCEPT_CONTENT);
    }

    private void doDownload() {
        if (!isNetworkAvailable()) {
            MessageHelpers.showMessage(mContext, "Internet connection not available!");
        }

        String url = mRequest.mDownloadUri.toString();

        Log.d(TAG, "Starting download %s...", url);

        Response response = OkHttpManager.instance().doRequest(url, mClient, mHeaders);

        if (response == null || response.body() == null) {
            throw new IllegalStateException("Error: bad response");
        }

        mResponseStream = new BufferedInputStream(response.body().byteStream());

        if (mRequest.mDestinationUri != null) {
             
            mFileUri = streamToFile(mResponseStream, getDestination());
        }
    }

     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     

    private void doDownload2() {
        if (!isNetworkAvailable()) {
            MessageHelpers.showMessage(mContext, "Internet connection not available!");
        }

        String url = mRequest.mDownloadUri.toString();

        Request request = new Request.Builder()
                .url(url)
                .build();

        for (int tries = NUM_TRIES; tries > 0; tries--) {
            try {
                Response response = mClient.newCall(request).execute();
                if (!response.isSuccessful()) throw new IllegalStateException("Unexpected code " + response);

                 
                mResponseStream = new ByteArrayInputStream(response.body().bytes());
                break;  
            } catch (SocketTimeoutException | UnknownHostException ex) {
                if (tries == 1)  
                    throw new IllegalStateException(ex);
            } catch (IOException | RuntimeException ex) {
                throw new IllegalStateException(ex);
            }
        }

    }

    private OkHttpClient createOkHttpClient() {
        Interceptor intercept = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Response originalResponse = chain.proceed(chain.request());
                return originalResponse.newBuilder().body(new ProgressResponseBody(originalResponse.body(), mRequest.mProgressListener)).build();
            }
        };

         
         
         
         

        Builder builder = OkHttpManager.instance().getClient().newBuilder()
                .addNetworkInterceptor(intercept);

        return builder.build();
    }

    private Uri streamToFile(InputStream is, Uri destination) {
        if (destination == null) {
            return null;
        }

        FileOutputStream fos = null;

        try {
            long downStartTimeMs = System.currentTimeMillis();

            fos = new FileOutputStream(destination.getPath());

            byte[] buffer = new byte[1024];
            int len1;
            int totalLen = 0;
            while ((len1 = is.read(buffer)) != -1) {
                totalLen += len1;
                fos.write(buffer, 0, len1);

                if (System.currentTimeMillis() - downStartTimeMs > MAX_DOWN_TIME_MS) {
                     
                    totalLen = 0;
                    break;
                }
            }

            mTotalLen = totalLen;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        } finally {
            FileHelpers.closeStream(fos);
            FileHelpers.closeStream(is);
        }

        return destination;
    }

     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     
     

    private Uri getDestination() {
        if (mRequest.mDestinationUri != null) {
            return mRequest.mDestinationUri;
        }

        File cacheDir = FileHelpers.getCacheDir(mContext);

        if (cacheDir == null) {
            return null;
        }

        File outputFile = new File(cacheDir, "tmp_file");
        return Uri.fromFile(outputFile);
    }

    public long enqueue(MyRequest request) {
        mFileUri = null;
        mRequest = request;
        mRequestId = new Random().nextLong();

        doDownload();

        return mRequestId;
    }

     
     
     
     
     
     
     
     
     
     
     
     
     

    public void remove(long downloadId) {
        mClient.dispatcher().cancelAll();
    }

    public Uri getUriForDownloadedFile(long requestId) {
        if (mFileUri == null) {
            mFileUri = streamToFile(mResponseStream, getDestination());
        }

        return mFileUri;
    }

     
    public int getSizeForDownloadedFile(long requestId) {
        return mTotalLen;
    }

    public InputStream getStreamForDownloadedFile(long requestId) {
        return mResponseStream;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private static class ProgressResponseBody extends ResponseBody {

        private final ResponseBody responseBody;
        private final ProgressListener progressListener;
        private BufferedSource bufferedSource;

        ProgressResponseBody(ResponseBody responseBody, ProgressListener progressListener) {
            this.responseBody = responseBody;
            this.progressListener = progressListener;
        }

        @Override
        public MediaType contentType() {
            return responseBody.contentType();
        }

        @Override
        public long contentLength() {
            return responseBody.contentLength();
        }

        @Override
        public BufferedSource source() {
            if (bufferedSource == null) {
                bufferedSource = Okio.buffer(source(responseBody.source()));
            }
            return bufferedSource;
        }

        private Source source(Source source) {
            return new ForwardingSource(source) {
                long totalBytesRead = 0L;

                @Override
                public long read(Buffer sink, long byteCount) throws IOException {
                    long bytesRead = super.read(sink, byteCount);

                    if (progressListener == null) {
                        return bytesRead;
                    }

                     
                    totalBytesRead += bytesRead != -1 ? bytesRead : 0;
                    progressListener.update(totalBytesRead, responseBody.contentLength(), bytesRead == -1);
                    return bytesRead;
                }
            };
        }
    }

    public interface ProgressListener {
        void update(long bytesRead, long contentLength, boolean done);
    }

    public static class MyRequest {
        private final Uri mDownloadUri;
        private Uri mDestinationUri;
        private ProgressListener mProgressListener;

        public MyRequest(Uri uri) {
            mDownloadUri = uri;
        }

        public void setDestinationUri(Uri uri) {
            mDestinationUri = uri;
        }

        public void setProgressListener(ProgressListener listener) {
            mProgressListener = listener;
        }
    }
}
