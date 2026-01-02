package com.liskovsoft.sharedutils.mylogger;

abstract class MyLogger {
    public void i(String tag, String msg) {}
    public void d(String tag, String msg) {}
    public void w(String tag, String msg) {}
    public void e(String tag, String msg) {}
    public void flush() {}

    public abstract String getLogType();
}
