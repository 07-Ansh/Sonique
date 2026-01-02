package com.liskovsoft.sharedutils.configparser;

public interface ConfigParser {
    String get(String key);
    boolean getBoolean(String key);
    String[] getArray(String key);
}
