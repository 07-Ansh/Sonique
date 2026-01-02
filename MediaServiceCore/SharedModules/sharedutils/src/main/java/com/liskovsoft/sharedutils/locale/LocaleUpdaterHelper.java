package com.liskovsoft.sharedutils.locale;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build.VERSION;
import android.os.LocaleList;
import com.liskovsoft.sharedutils.helpers.Helpers;

import java.util.Locale;
import java.util.StringTokenizer;

class LocaleUpdaterHelper {
    private static final String LOCALE_EN_US = "en_US";
    private static final String LOCALE_RU = "ru_RU";
    private final static String[] rusPackages = {"dkc.androidtv.tree", "dkc.video.fsbox", "dkc.video.hdbox", "dkc.video.uatv"};

    public static String guessLocale(Context context) {
        if (isRussianPackagesInstalled(context)) {
            return LOCALE_RU;
        }

        if (isChineseDevice()) {
            return LOCALE_EN_US;
        }

        return null;
    }

    private static boolean isRussianPackagesInstalled(Context context) {
        for (ApplicationInfo info : Helpers.getInstalledPackages(context)) {
            if (isRussianPackage(info.packageName)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isRussianPackage(String pkgName) {
        for (String rusPackage : rusPackages) {
            if (rusPackage.equals(pkgName)){
                return true;
            }
        }
        return false;
    }

    private static boolean isChineseDevice() {
        String deviceName = Helpers.getDeviceName();

        switch (deviceName) {
            case "ChangHong Android TV (full_mst638)":
                return true;
        }

        return false;
    }

    private static boolean isChineseLocale(Context context) {
        String script = LocaleUtility.getScript(Locale.getDefault());

        if (isChineseScript(script)) {
            return true;
        }

        return false;
    }

    private static boolean isChineseScript(String script) {
        switch (script) {
            case "Hani":
            case "Hans":
            case "Hant":
                return true;
        }
        return false;
    }

    // short lang code. ex: "ru"
    public static void forceLocale(Context context, String langCode) {
        if (langCode == null || langCode.isEmpty()) {
            return;
        }

        Locale locale = parseLangCode(langCode);
        Locale oldLocale = Locale.getDefault();
        if (oldLocale.equals(locale)) {
            return;
        }

        Locale.setDefault(locale);
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        config.locale = locale;

        if (VERSION.SDK_INT >= 24) {
            config.setLocales(new LocaleList(locale));
        }

        resources
                .updateConfiguration(
                        config,
                        resources.getDisplayMetrics()
                );
    }

    public static Locale parseLangCode(String langCode) {
        if (langCode == null) {
            return null;
        }

        StringTokenizer tokenizer = new StringTokenizer(langCode, "_");
        String lang = tokenizer.nextToken();
        String country = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "";
        return new Locale(lang, country);
    }

    public static String getDefaultLocale() {
        Locale defaultLocale = Locale.getDefault();
        String lang = defaultLocale.getLanguage();
        String country = defaultLocale.getCountry();

        if (country == null || country.isEmpty()) {
            return String.format("%s", lang);
        }

        return String.format("%s_%S", lang, country);
    }
}
