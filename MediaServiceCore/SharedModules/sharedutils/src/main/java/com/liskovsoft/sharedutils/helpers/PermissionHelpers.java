package com.liskovsoft.sharedutils.helpers;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.provider.Settings;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

@TargetApi(16)
public class PermissionHelpers {
     
    public static final int REQUEST_EXTERNAL_STORAGE = 112;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

     
    public static final int REQUEST_MIC = 113;
    private static String[] PERMISSIONS_MIC = {
            Manifest.permission.RECORD_AUDIO
    };

     
    public static final int REQUEST_OVERLAY = 114;
    private static String[] PERMISSIONS_OVERLAY = {
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION
    };

     
    public static void verifyStoragePermissions(Context context) {
        requestPermissions(context, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
    }

    public static void verifyMicPermissions(Context context) {
        requestPermissions(context, PERMISSIONS_MIC, REQUEST_MIC);
    }

    public static void verifyOverlayPermissions(Context context) {
        if (Build.VERSION.SDK_INT >= 29 && !Settings.canDrawOverlays(context) && context instanceof Activity) {
             
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.getApplicationContext().getPackageName())
            );
            ((Activity) context).startActivityForResult(intent, REQUEST_OVERLAY);
        }
    }

     
    public static boolean hasStoragePermissions(Context context) {
         
        return hasPermissions(context, PERMISSIONS_STORAGE);
    }

    public static boolean hasMicPermissions(Context context) {
         
        return hasPermissions(context, PERMISSIONS_MIC);
    }

    public static boolean hasOverlayPermissions(Context context) {
        return Build.VERSION.SDK_INT < 29 || Settings.canDrawOverlays(context) || !hasOverlayActivity(context);
    }

    private static boolean hasOverlayActivity(Context context) {
        if (Build.VERSION.SDK_INT >= 29) {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.getApplicationContext().getPackageName())
            );

            PackageManager packageManager = context.getPackageManager();

            return intent.resolveActivity(packageManager) != null;
        }

        return false;
    }

     

     
    private static void requestPermissions(Context activity, String[] permissions, int requestId) {
        if (!hasPermissions(activity, permissions) && !Helpers.isGenymotion()) {
            if (activity instanceof Activity) {
                 
                ActivityCompat.requestPermissions(
                        (Activity) activity,
                        permissions,
                        requestId
                );
            }
        }
    }

     
    private static boolean hasPermissions(@Nullable Context context, String... permissions) {
        if (context == null) {
            return false;
        }

        if (VERSION.SDK_INT >= 23) {
            for (String permission : permissions) {
                int result = ActivityCompat.checkSelfPermission(context, permission);
                if (result != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean hasPermission(@Nullable Context context, String permission) {
        if (context == null) {
            return false;
        }

        return PackageManager.PERMISSION_GRANTED == context.getPackageManager().checkPermission(
                permission, context.getPackageName());
    }
}
