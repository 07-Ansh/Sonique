package com.liskovsoft.sharedutils.dialogs;

import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import androidx.appcompat.app.AlertDialog;
import com.liskovsoft.sharedutils.R;

public class YesNoDialog {
    public static void create(Context context, int msgResId, OnClickListener listener, int themeResId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, themeResId);
        builder
                .setMessage(msgResId)
                .setTitle(context.getApplicationInfo().labelRes)
                .setPositiveButton(R.string.yes_btn, listener)
                .setNegativeButton(R.string.no_btn, listener)
                .show();
    }

    public static void create(Context context, String message, OnClickListener listener, int themeResId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, themeResId);
        builder
                .setMessage(message)
                .setTitle(context.getApplicationInfo().labelRes)
                .setPositiveButton(R.string.yes_btn, listener)
                .setNegativeButton(R.string.no_btn, listener)
                .show();
    }
}
