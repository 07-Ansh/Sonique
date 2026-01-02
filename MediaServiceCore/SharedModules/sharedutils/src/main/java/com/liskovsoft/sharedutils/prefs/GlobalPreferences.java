package com.liskovsoft.sharedutils.prefs;

import android.annotation.SuppressLint;
import android.content.Context;

import com.liskovsoft.sharedutils.helpers.DateHelper;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

final public class GlobalPreferences extends SharedPreferencesBase {
    public static final String PLAYLIST_TYPE_RECOMMENDATIONS = "playlist_type_recommendations";
    public static final String PLAYLIST_TYPE_SUBSCRIPTIONS = "playlist_type_subscriptions";
    public static final String PLAYLIST_TYPE_HISTORY = "playlist_type_history";
    @SuppressLint("StaticFieldLeak")
    public static GlobalPreferences sInstance;
    private static final String SHARED_PREFERENCES_NAME = GlobalPreferences.class.getName();
    private final static String MEDIA_SERVICE_ACCOUNT_DATA = "media_service_account_data";
    private final static String OAUTH2_ACCOUNT_DATA = "oauth2_account_data";
    private final static String MEDIA_SERVICE_DATA = "media_service_data";
    private static final String RECOMMENDED_PLAYLIST_TYPE = "recommended_playlist_type";
    private static final String PREFERRED_LANGUAGE_DATA = "preferred_language_data";
    private static final String PREFERRED_COUNTRY_DATA = "preferred_country_data";
    private static final String ENABLE_CHANNELS_SERVICE = "enable_channels_service";
    private static final String PREFER_IPV_4_DNS = "prefer_ipv4_dns";
    private static final String CONTENT_BLOCK_ALT_SERVER = "content_block_alt_server";
    private static final String IS_24_HOUR_LOCALE_ENABLED = "is_24_hour_locale_enabled";
    private static final List<Runnable> sCallbacks = new CopyOnWriteArrayList<>(); // fix ConcurrentModificationException

    private GlobalPreferences(Context context) {
        super(context, SHARED_PREFERENCES_NAME);
    }

    public static GlobalPreferences instance(Context context) {
        if (sInstance == null) {
            sInstance = new GlobalPreferences(context.getApplicationContext());

            for (Runnable callback : sCallbacks) {
                new Thread(callback).start(); // fix network on main thread exception
            }

            // make callbacks garbage collected
            sCallbacks.clear();
        }

        return sInstance;
    }

    public static void setOnInit(Runnable callback) {
        // Fix lost account after reboot bug
        if (sInstance == null) {
            sCallbacks.add(callback);
        } else {
            new Thread(callback).start(); // fix network on main thread exception
        }
    }

    public static boolean isInitialized() {
        return sInstance != null && sInstance.getContext() != null;
    }

    public String getRecommendedPlaylistType() {
        return getString(RECOMMENDED_PLAYLIST_TYPE, PLAYLIST_TYPE_RECOMMENDATIONS);
    }

    public void setRecommendedPlaylistType(String type) {
        putString(RECOMMENDED_PLAYLIST_TYPE, type);
    }

    public String getMediaServiceAccountData() {
        return getString(MEDIA_SERVICE_ACCOUNT_DATA, null);
    }

    public void setMediaServiceAccountData(String data) {
        putString(MEDIA_SERVICE_ACCOUNT_DATA, data);
    }

    public String getOAuth2AccountData() {
        return getString(OAUTH2_ACCOUNT_DATA, null);
    }

    public void setOAuth2AccountData(String data) {
        putString(OAUTH2_ACCOUNT_DATA, data);
    }

    public String getMediaServiceData() {
        return getString(MEDIA_SERVICE_DATA, null);
    }

    public void setMediaServiceData(String data) {
        putString(MEDIA_SERVICE_DATA, data);
    }

    public String getPreferredLanguage() {
        return getString(PREFERRED_LANGUAGE_DATA, null);
    }

    public void setPreferredLanguage(String langData) {
        putString(PREFERRED_LANGUAGE_DATA, langData);
    }

    public String getPreferredCountry() {
        return getString(PREFERRED_COUNTRY_DATA, null);
    }

    public void setPreferredCountry(String countryData) {
        putString(PREFERRED_COUNTRY_DATA, countryData);
    }

    public boolean isChannelsServiceEnabled() {
        return getBoolean(ENABLE_CHANNELS_SERVICE, true);
    }

    public void setChannelsServiceEnabled(boolean enable) {
        putBoolean(ENABLE_CHANNELS_SERVICE, enable);
    }

    public boolean isIPv4DnsPreferred() {
        return getBoolean(PREFER_IPV_4_DNS, false);
    }

    public void setIPv4DnsPreferred(boolean enable) {
        putBoolean(PREFER_IPV_4_DNS, enable);
    }

    public boolean isContentBlockAltServerEnabled() {
        return getBoolean(CONTENT_BLOCK_ALT_SERVER, false);
    }

    public void setContentBlockAltServerEnabled(boolean enable) {
        putBoolean(CONTENT_BLOCK_ALT_SERVER, enable);
    }

    public boolean is24HourLocaleEnabled() {
        return getBoolean(IS_24_HOUR_LOCALE_ENABLED, DateHelper.is24HourLocale());
    }

    public void set24HourLocaleEnabled(boolean enable) {
        putBoolean(IS_24_HOUR_LOCALE_ENABLED, enable);
    }
}
