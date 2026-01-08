package com.liskovsoft.sharedutils.helpers;

import android.os.Build;

import com.liskovsoft.sharedutils.prefs.GlobalPreferences;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

 
public class DateHelper {
     
    public static long toUnixTimeMs(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return 0;
        }

         
        boolean supportXPattern = Build.VERSION.SDK_INT > 23;
        String longPattern = "yyyy-MM-dd'T'HH:mm:ss" + (timestamp.contains("+") && supportXPattern ? "X" : "");
        String shortPattern = "yyyy-MM-dd";
        SimpleDateFormat format = new SimpleDateFormat(timestamp.contains("T") ? longPattern : shortPattern, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = null;
        try {
            date = format.parse(timestamp);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date != null ? date.getTime() : 0;
    }

     
    private static Date toDate(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return null;
        }

         
        boolean supportXPattern = Build.VERSION.SDK_INT > 23;
        String longPattern = "yyyy-MM-dd'T'HH:mm:ss" + (timestamp.contains("+") && supportXPattern ? "X" : "");
        String shortPattern = "yyyy-MM-dd";
        SimpleDateFormat format = new SimpleDateFormat(timestamp.contains("T") ? longPattern : shortPattern, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = null;
        try {
            date = format.parse(timestamp);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    public static String getCurrentDateTimeShort() {
        return toShortDate(System.currentTimeMillis(), true, false, true);
    }

    public static String getCurrentTimeShort() {
        return toShortDate(System.currentTimeMillis(), false, false, true);
    }

    public static String getCurrentDateShort() {
        return toShortDate(System.currentTimeMillis(), true, false, false);
    }

    public static String toShortTime(long timeMs) {
        return toShortDate(timeMs, false, false, true);
    }

    public static String toShortDate(long timeMs, boolean showDate, boolean showYear, boolean showHours) {
        return toShortDate(new Date(timeMs), showDate, showYear, showHours);
    }

     
    public static String toShortDate(String timestamp, boolean showDate, boolean showYear, boolean showHours) {
        return toShortDate(toDate(timestamp), showDate, showYear, showHours);
    }

    private static String toShortDate(Date date, boolean showDate, boolean showYear, boolean showHours) {
        Locale locale = Locale.getDefault();
        boolean is24HourLocale = GlobalPreferences.sInstance != null ? GlobalPreferences.sInstance.is24HourLocaleEnabled() : is24HourLocale(locale);
        String datePattern = is24HourLocale ? "EEE d MMM" : "EEE MMM d";
        String yearPattern = "y";
        String hoursPattern = is24HourLocale ? "H:mm" : "h:mm a";

        datePattern = showDate ? datePattern : "";
        yearPattern = showYear ? yearPattern : "";
        hoursPattern = showHours ? hoursPattern : "";

         
        SimpleDateFormat sdf = new SimpleDateFormat(Helpers.combineItems(" ", datePattern, yearPattern, hoursPattern), locale);

        return sdf.format(date);
    }

    public static boolean is24HourLocale() {
        return is24HourLocale(Locale.getDefault());
    }

    private static boolean is24HourLocale(Locale locale) {
        return !Helpers.equalsAny(locale.getLanguage(), "en", "es", "pt", "fr", "hi", "tl", "ar", "sw", "bn", "ur");
    }
}
