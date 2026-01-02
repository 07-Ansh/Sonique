package com.liskovsoft.sharedutils.helpers;

import android.os.Build;

import com.liskovsoft.sharedutils.prefs.GlobalPreferences;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Format variants:<br/>
 * "dd.MM.yyyy HH:mm"<br/>
 * "MM/dd/yyyy hh:mm a"<br/>
 * "d MMM, y"<br/>
 * Manual: https://jenkov.com/tutorials/java-internationalization/simpledateformat.html#pattern-syntax
 */
public class DateHelper {
    /**
     * Input example: "2022-09-11T23:39:38+00:00"<br/>
     * https://stackoverflow.com/questions/2597083/illegal-pattern-character-t-when-parsing-a-date-string-to-java-util-date<br/>
     * https://stackoverflow.com/questions/7681782/simpledateformat-unparseable-date-exception
     */
    public static long toUnixTimeMs(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return 0;
        }

        // Unknown pattern character 'X'
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

    /**
     * Input example: "2022-09-11T23:39:38+00:00"<br/>
     * https://stackoverflow.com/questions/2597083/illegal-pattern-character-t-when-parsing-a-date-string-to-java-util-date<br/>
     * https://stackoverflow.com/questions/7681782/simpledateformat-unparseable-date-exception
     */
    private static Date toDate(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return null;
        }

        // Unknown pattern character 'X'
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

    /**
     * Input example: "2022-09-11T23:39:38+00:00"<br/>
     * https://stackoverflow.com/questions/2597083/illegal-pattern-character-t-when-parsing-a-date-string-to-java-util-date<br/>
     * https://stackoverflow.com/questions/7681782/simpledateformat-unparseable-date-exception
     */
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

        // details: https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
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
