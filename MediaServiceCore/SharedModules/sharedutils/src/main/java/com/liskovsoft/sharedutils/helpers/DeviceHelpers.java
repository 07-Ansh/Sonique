package com.liskovsoft.sharedutils.helpers;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.webkit.CookieManager;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;

public final class DeviceHelpers {
    private static final String AMAZON_FEATURE_FIRE_TV = "amazon.hardware.fire_tv";
    private static final boolean SAMSUNG = Build.MANUFACTURER.equals("samsung");
    private static Boolean isTV = null;
    private static Boolean isFireTV = null;
    private static int sMaxHeapMemoryMB = -1;

     
    public static final int MEDIA_TUNNELING_DEVICE_BLACKLIST_VERSION = 994;

     
     
    private static final boolean HI3798MV200 = Build.VERSION.SDK_INT == 24
            && Build.DEVICE.equals("Hi3798MV200");
     
    private static final boolean CVT_MT5886_EU_1G = Build.VERSION.SDK_INT == 24
            && Build.DEVICE.equals("cvt_mt5886_eu_1g");
     
    private static final boolean REALTEKATV = Build.VERSION.SDK_INT == 25
            && Build.DEVICE.equals("RealtekATV");
     
    private static final boolean PH7M_EU_5596 = Build.VERSION.SDK_INT >= 26
            && Build.DEVICE.equals("PH7M_EU_5596");
     
    private static final boolean QM16XE_U = Build.VERSION.SDK_INT == 23
            && Build.DEVICE.equals("QM16XE_U");
     
    private static final boolean BRAVIA_VH1 = Build.VERSION.SDK_INT == 29
            && Build.DEVICE.equals("BRAVIA_VH1");
     
    private static final boolean BRAVIA_VH2 = Build.VERSION.SDK_INT == 29
            && Build.DEVICE.equals("BRAVIA_VH2");
     
    private static final boolean BRAVIA_ATV2 = Build.DEVICE.equals("BRAVIA_ATV2");
     
    private static final boolean BRAVIA_ATV3_4K = Build.DEVICE.equals("BRAVIA_ATV3_4K");
     
    private static final boolean TX_50JXW834 = Build.DEVICE.equals("TX_50JXW834");
     
    private static final boolean HMB9213NW = Build.DEVICE.equals("HMB9213NW");
     

    private DeviceHelpers() {
    }

    public static boolean isConfirmKey(final int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                return true;
            default:
                return false;
        }
    }

    public static int dpToPx(@Dimension(unit = Dimension.DP) final int dp,
                             @NonNull final Context context) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics());
    }

    public static int spToPx(@Dimension(unit = Dimension.SP) final int sp,
                             @NonNull final Context context) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                sp,
                context.getResources().getDisplayMetrics());
    }

    public static boolean isLandscape(final Context context) {
        return context.getResources().getDisplayMetrics().heightPixels < context.getResources()
                .getDisplayMetrics().widthPixels;
    }

    public static boolean hasAnimationsAnimatorDurationEnabled(final Context context) {
        return Settings.System.getFloat(
                context.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1F) != 0F;
    }

     
    public static boolean shouldSupportMediaTunneling() {
         
        return !HI3798MV200
                && !CVT_MT5886_EU_1G
                && !REALTEKATV
                && !QM16XE_U
                && !BRAVIA_VH1
                && !BRAVIA_VH2
                && !BRAVIA_ATV2
                && !BRAVIA_ATV3_4K
                && !PH7M_EU_5596
                && !TX_50JXW834
                && !HMB9213NW;
    }

     
    public static boolean isWebViewSupported() {
        try {
            CookieManager.getInstance();
            return !isWebViewBroken();
        } catch (final Throwable ignored) {
            return false;
        }
    }

    private static boolean isWebViewBroken() {
        return Build.VERSION.SDK_INT == 19 && isTCL();  
    }

    private static boolean isTCL() {
        return Build.MANUFACTURER.toLowerCase().contains("tcl") || Build.BRAND.toLowerCase().contains("tcl");
    }

    @SuppressWarnings("deprecation")
    public static String getPrimaryAbi() {
        String primaryAbi;

        if (Build.VERSION.SDK_INT >= 21) {
             
            String[] abis = Build.SUPPORTED_ABIS;
            if (abis != null && abis.length > 0) {
                primaryAbi = abis[0];
            } else {
                primaryAbi = Build.CPU_ABI;
            }
        } else {
            primaryAbi = Build.CPU_ABI;
        }

        return primaryAbi;
    }

    public static int getMaxHeapMemoryMB() {
        if (sMaxHeapMemoryMB == -1) {
            long maxMemory = Runtime.getRuntime().maxMemory();
            sMaxHeapMemoryMB = (int)(maxMemory / (1024 * 1024));  
        }

        return sMaxHeapMemoryMB;
    }

    public static int getAllocatedHeapMemoryMB() {
        long allocatedMemory = Runtime.getRuntime().totalMemory();
        return (int)(allocatedMemory / (1024 * 1024));
    }

    public static boolean isMemoryCritical() {
        return getAllocatedHeapMemoryMB() > getMaxHeapMemoryMB() * 0.5;
    }
}
