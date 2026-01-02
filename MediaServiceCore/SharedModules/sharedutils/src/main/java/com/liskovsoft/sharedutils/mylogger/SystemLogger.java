package com.liskovsoft.sharedutils.mylogger;

import android.util.Log;

class SystemLogger extends MyLogger {
    @Override
    public void d(String tag, String msg) {
        if (msg != null) {
            Log.d(tag, msg);
        }
    }

    @Override
    public void i(String tag, String msg) {
        if (msg != null) {
            Log.i(tag, msg);
        }
    }

    @Override
    public void w(String tag, String msg) {
        if (msg != null) {
            Log.w(tag, msg);
        }
    }

    @Override
    public void e(String tag, String msg) {
        if (msg != null) {
            Log.e(tag, msg);
        }
    }

    @Override
    public String getLogType() {
        return com.liskovsoft.sharedutils.mylogger.Log.LOG_TYPE_SYSTEM;
    }
}
