package com.liskovsoft.sharedutils.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.liskovsoft.sharedutils.mylogger.Log;

import java.io.File;
import java.lang.ref.WeakReference;

public class SharedPreferencesBase {
    private static final String TAG = SharedPreferencesBase.class.getSimpleName();
    private static final long PREF_MAX_SIZE_MB = 5;
    private final SharedPreferences mPrefs;
    protected WeakReference<Context> mContext = new WeakReference<>(null);

    public SharedPreferencesBase(Context context, String prefName) {
        this(context, prefName, -1, false);
    }

    public SharedPreferencesBase(Context context, String prefName, boolean limitMaxSize) {
        this(context, prefName, -1, limitMaxSize);
    }

    public SharedPreferencesBase(Context context, String prefName, int defValResId) {
        this(context, prefName, defValResId, false);
    }

    public SharedPreferencesBase(Context context) {
        this(context, null, -1, false);
    }

    public SharedPreferencesBase(Context context, int defValResId) {
        this(context, null, defValResId, false);
    }

    public SharedPreferencesBase(Context context, String prefName, int defValResId, boolean limitMaxSize) {
        if (limitMaxSize) {
            limitMaxSize(context, prefName);
        }

        setContext(context);

        if (prefName != null) {
            mPrefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE);
        } else {
            prefName = context.getPackageName() + "_preferences";
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        }

        if (defValResId != -1) {
            PreferenceManager.setDefaultValues(context, prefName, Context.MODE_PRIVATE, defValResId, true);
        }
    }

    /**
     * Delete prefs which size exceeds a limit to prevent unconditional behavior
     */
    private void limitMaxSize(Context context, String prefName) {
        File sharedPrefs = new File(context.getApplicationInfo().dataDir, "shared_prefs" + "/" + prefName + ".xml");

        if (sharedPrefs.exists() && sharedPrefs.isFile()) {
            long sizeMB = sharedPrefs.length() / 1024 / 1024;

            if (sizeMB > PREF_MAX_SIZE_MB) {
                Log.e(TAG, "Shared preference max size exceeded. Deleting...");
                sharedPrefs.delete();
            }
        }
    }

    @Nullable
    public Context getContext() {
        return mContext.get();
    }

    private void setContext(Context context) {
        if (context != null) {
            mContext = new WeakReference<>(context);
        }
    }

    public void putLong(String key, long val) {
        mPrefs.edit()
                .putLong(key, val)
                .apply();
    }

    public long getLong(String key, long defVal) {
        return mPrefs.getLong(key, defVal);
    }

    public void putInt(String key, int val) {
        mPrefs.edit()
                .putInt(key, val)
                .apply();
    }

    public int getInt(String key, int defVal) {
        return mPrefs.getInt(key, defVal);
    }

    public void putBoolean(String key, boolean val) {
        mPrefs.edit()
                .putBoolean(key, val)
                .apply();
    }

    public boolean getBoolean(String key, boolean defVal) {
        return mPrefs.getBoolean(key, defVal);
    }

    public void putString(String key, String  val) {
        mPrefs.edit()
                .putString(key, val)
                .apply();
    }

    public String getString(String key, String defVal) {
        return mPrefs.getString(key, defVal);
    }

    public void clear() {
        mPrefs.edit()
                .clear()
                .apply();
    }
}
