package com.liskovsoft.sharedutils.locale;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.sharedutils.prefs.GlobalPreferences;

import java.util.Locale;
import java.util.StringTokenizer;

public class LocaleUpdater {
    private static final String TAG = LocaleUpdater.class.getSimpleName();
    private static Locale sCachedLocale;
    private final GlobalPreferences mPrefs;
    private final Context mContext;

    public LocaleUpdater(Context context) {
        mContext = context;
        mPrefs = GlobalPreferences.instance(context);
    }

    public void update() {
        String locale = getUpdatedLocale();

        Log.d(TAG, "Updating locale to " + locale);

        LocaleUpdaterHelper.forceLocale(mContext, locale);
    }

    private String getUpdatedLocale() {
        try {
            return createLocale();
        } catch (NullPointerException e) {
            // NullPointerException: com.liskovsoft.sharedutils.locale.LocaleUtility.getCurrentLocale (LocaleUtility.java:772)
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Get locale in http format (e.g. zh, ru-RU etc)<br/>
     * Example: <code>ru,en-US;q=0.9,en;q=0.8,uk;q=0.7</code>
     * @return lang code
     */
    public String getPreferredBrowserLocale() {
        String locale = getPreferredLanguage();

        if (locale == null || locale.isEmpty()) {
            locale = LocaleUpdaterHelper.getDefaultLocale();
        }

        return locale.replace("_", "-").toLowerCase();
    }

    /**
     * Get locale as lang code (e.g. zh, ru_RU etc)
     * @return lang code
     */
    public String getPreferredLanguage() {
        String language = mPrefs.getPreferredLanguage();

        return language != null ? language : "";
    }

    /**
     * E.g. ru, uk, en
     * @param langCode lang
     */
    public void setPreferredLanguage(String langCode) {
        mPrefs.setPreferredLanguage(langCode);
    }

    public String getPreferredCountry() {
        String country = mPrefs.getPreferredCountry();
        return country != null ? country : "";
    }

    public void setPreferredCountry(String countryCode) {
        mPrefs.setPreferredCountry(countryCode);
    }

    private String createLocale() {
        String langCode = getPreferredLanguage();
        String countryCode = getPreferredCountry();

        boolean isLangCodeEmpty = langCode == null || langCode.isEmpty();
        boolean isCountryCodeEmpty = countryCode == null || countryCode.isEmpty();

        if (isLangCodeEmpty && isCountryCodeEmpty) {
            return null;
        }

        Locale currentLocale = null;

        if (isLangCodeEmpty || isCountryCodeEmpty) {
            currentLocale  = LocaleUtility.getCurrentLocale(mContext);
        }

        if (isLangCodeEmpty) {
            langCode = currentLocale.getLanguage();
        } else {
            StringTokenizer tokenizer = new StringTokenizer(langCode, "_");
            langCode = tokenizer.nextToken();
        }

        if (isCountryCodeEmpty) {
            countryCode = currentLocale.getCountry();
        } else {
            StringTokenizer tokenizer = new StringTokenizer(countryCode, "_");
            countryCode = tokenizer.nextToken();
            if (tokenizer.hasMoreTokens()) {
                countryCode = tokenizer.nextToken(); // E.g. fr_BE
            }
        }

        return String.format("%s_%s", langCode, countryCode);
    }

    public static Locale getSavedLocale(Context context) {
        if (sCachedLocale == null) {
            LocaleUpdater updater = new LocaleUpdater(context);
            String langCode = updater.getUpdatedLocale();
            sCachedLocale = LocaleUpdaterHelper.parseLangCode(langCode);
        }

        return sCachedLocale;
    }

    @SuppressWarnings("deprecation")
    public static void applySavedLocale(Context context) {
        if (context == null) {
            return;
        }

        Locale newLocale = LocaleUpdater.getSavedLocale(context);

        if (newLocale == null) {
            return;
        }

        Resources res = context.getResources();

        if (res == null) {
            return;
        }

        Configuration configuration = res.getConfiguration();

        configuration.locale = newLocale;
        res.updateConfiguration(configuration, res.getDisplayMetrics());
    }

    public static void clearCache() {
        sCachedLocale = null;
    }
}
