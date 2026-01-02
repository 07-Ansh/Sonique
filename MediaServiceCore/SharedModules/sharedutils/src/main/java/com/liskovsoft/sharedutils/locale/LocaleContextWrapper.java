package com.liskovsoft.sharedutils.locale;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build.VERSION;
import android.os.LocaleList;
import android.util.DisplayMetrics;

import java.util.Locale;

/**
 * <a href="https://stackoverflow.com/questions/4985805/set-locale-programmatically">More info</a>
 */
public class LocaleContextWrapper extends ContextWrapper {
    public LocaleContextWrapper(Context base) {
        super(base);
    }

    public static Context wrap(Context context, Locale newLocale) {
        return wrap(context, newLocale, null);
    }
    
    public static Context wrap(Context context, Locale newLocale, DisplayMetrics customMetrics) {
        if (newLocale == null) {
            return context;
        }

        Resources res = context.getResources();

        if (res == null) {
            return context;
        }

        Configuration configuration = res.getConfiguration();

        if (customMetrics != null) {
            configuration.densityDpi = (int) (customMetrics.density * 160); // 160 is the baseline DPI
        }

        if (VERSION.SDK_INT >= 24) {
            configuration.setLocale(newLocale);

            LocaleList localeList = new LocaleList(newLocale);
            LocaleList.setDefault(localeList);
            configuration.setLocales(localeList);

            context = context.createConfigurationContext(configuration);
        } else if (VERSION.SDK_INT >= 17) {
            configuration.setLocale(newLocale);
            context = context.createConfigurationContext(configuration);
        } else {
            configuration.locale = newLocale;
            res.updateConfiguration(configuration, res.getDisplayMetrics());
        }

        return new ContextWrapper(context);
    }

    public static void applySavedLocale(Context context, Locale newLocale, DisplayMetrics customMetrics) {
        if (context == null) {
            return;
        }

        if (newLocale == null) {
            return;
        }

        Resources res = context.getResources();

        if (res == null) {
            return;
        }

        Configuration configuration = res.getConfiguration();

        if (customMetrics != null) {
            configuration.densityDpi = (int) (customMetrics.density * 160); // 160 is the baseline DPI
        }

        configuration.locale = newLocale;
        res.updateConfiguration(configuration, res.getDisplayMetrics());
    }
}
