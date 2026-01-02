package com.liskovsoft.sharedutils.okhttp.interceptors;

import com.liskovsoft.sharedutils.mylogger.Log;
import okhttp3.Interceptor;
import okhttp3.Response;

import java.io.IOException;

public class RateLimitInterceptor implements Interceptor {
    private static final String TAG = RateLimitInterceptor.class.getSimpleName();

    @Override
    public Response intercept(Chain chain) throws IOException {
        Log.d(TAG, "Intercepting....");

        Response response = chain.proceed(chain.request());

        // 429 is how the api indicates a rate limit error
        if (!response.isSuccessful() && response.code() == 429) {
            Log.e(TAG, "To much requests. Waiting..." + response.message());

            try {
                Thread.sleep(1_000);
            } catch (InterruptedException ignored) {
                // NOP
            }
        }

        return response;
    }
}
