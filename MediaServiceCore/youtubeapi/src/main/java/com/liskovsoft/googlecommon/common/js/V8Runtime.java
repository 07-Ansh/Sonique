package com.liskovsoft.googlecommon.common.js;

import androidx.annotation.Nullable;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8ResultUndefined;
import com.eclipsesource.v8.V8ScriptExecutionException;
import com.liskovsoft.sharedutils.mylogger.Log;

import java.util.List;

public final class V8Runtime {
    private static final String TAG = V8Runtime.class.getSimpleName();
    private static V8Runtime sInstance;
    private V8 mRuntime;

     
     
     
     

    private V8Runtime() {
    }

    public static V8Runtime instance() {
        if (sInstance == null) {
            sInstance = new V8Runtime();
        }

        return sInstance;
    }

    public static void unhold() {
         
         
         
         

        sInstance = null;
    }

    @Nullable
    public String evaluate(final String source) {
        try {
            return evaluateSafe(source);
        } catch (V8ScriptExecutionException e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    @Nullable
    public String evaluateWithErrors(final String source) throws V8ScriptExecutionException {
        return evaluateSafe(source);
    }

    @Nullable
    public String evaluate(final List<String> sources) {
        try {
            return evaluateSafe(sources);
        } catch (V8ScriptExecutionException e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    @Nullable
    public String evaluateWithErrors(final List<String> sources) throws V8ScriptExecutionException {
        return evaluateSafe(sources);
    }

     
    private String evaluateUnsafe(final String source) throws V8ScriptExecutionException {
        String result = null;

        try {
            if (mRuntime == null) {
                mRuntime = V8.createV8Runtime();
            }
            mRuntime.getLocker().acquire();  
            result = mRuntime.executeStringScript(source);
        } finally {
            if (mRuntime != null) {
                mRuntime.getLocker().release();  
            }
        }

        return result;
    }

     
    private String evaluateSafe(final String source) throws V8ScriptExecutionException {
        V8 runtime = null;
        String result;

        try {
            runtime = V8.createV8Runtime();
            result = runtime.executeStringScript(source);
        } finally {
            if (runtime != null) {
                runtime.release(false);
            }
        }

        return result;
    }

     
    private String evaluateSafe(final List<String> sources) throws V8ScriptExecutionException {
        V8 runtime = null;
        String result = null;

        try {
            runtime = V8.createV8Runtime();
            for (String source : sources) {
                try {
                    result = runtime.executeStringScript(source);
                } catch (V8ResultUndefined e) {
                     
                }
            }
        } finally {
            if (runtime != null) {
                runtime.release(false);
            }
        }

        return result;
    }
}
