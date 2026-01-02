package com.liskovsoft.sharedutils.helpers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.liskovsoft.sharedutils.R;

import java.util.ArrayList;
import java.util.List;

public class MessageHelpers {
    private static final int MAX_LEN = 300;
    private static final int LONG_MSG_TIMEOUT_MS = 5_000;
    private static final int CLEANUP_TIMEOUT_MS = 10_000;
    private static final List<Toast> sToasts = new ArrayList<>();
    private static final Runnable sCleanupContext = sToasts::clear;
    private static final Handler sHandler = new Handler(Looper.getMainLooper());
    private static long sExitMsgTimeMS;
    private static float sTextSize;

    public static void showMessage(final Context ctx, final String TAG, final Throwable ex) {
        showMessage(ctx, TAG + ": %s", Helpers.toString(ex));
    }

    public static void showMessage(final Context ctx, final String template, Object... params) {
        showMessage(ctx, String.format(template, params));
    }

    public static void showMessageThrottled(final Context ctx, final String msg) {
        // throttle msg calls
        if (System.currentTimeMillis() - sExitMsgTimeMS < LONG_MSG_TIMEOUT_MS) {
            return;
        }
        sExitMsgTimeMS = System.currentTimeMillis();
        showMessage(ctx, msg);
    }

    public static void showMessageThrottled(final Context ctx, final int msgResId) {
        showMessageThrottled(ctx, ctx.getString(msgResId));
    }

    public static void showMessage(final Context ctx, final String msg) {
        showMessage(ctx, msg, false);
    }

    public static void showMessage(final Context ctx, final String msg, final boolean isLong) {
        if (ctx == null || msg == null || msg.isEmpty()) {
            return;
        }

        final Context context = ctx.getApplicationContext(); // memory leak fix

        Runnable toast = () -> {
            try {
                Toast currentToast = Toast.makeText(context, Helpers.ellipsize(msg, MAX_LEN), Toast.LENGTH_LONG);
                fixTextSize(currentToast, context);
                addAndCancelPrevIfNeeded(currentToast, isLong);
                currentToast.show();

                setupCleanup();
            } catch (Exception ex) { // NPE fix
                ex.printStackTrace();
            }
        };

        if (Looper.myLooper() == Looper.getMainLooper()) {
            toast.run();
        } else {
            new Handler(Looper.getMainLooper()).post(toast);
        }
    }

    /**
     * Shows toast message.<br/>
     * Uses resource id as message.
     * @param ctx context
     * @param resId resource id
     */
    public static void showMessage(Context ctx, int resId) {
        if (ctx != null) {
            showMessage(ctx, ctx.getResources().getString(resId));
        }
    }

    /**
     * Shows formatted toast message.<br/>
     * Uses resource id as message.
     * @param ctx context
     * @param resId resource id
     * @param formatArgs format arguments
     */
    public static void showMessage(Context ctx, int resId, Object... formatArgs) {
        if (ctx != null) {
            showMessage(ctx, ctx.getResources().getString(resId, formatArgs));
        }
    }

    /**
     * Shows long toast message.<br/>
     * Uses resource id as message.
     * @param ctx context
     * @param resId resource id
     */
    public static void showLongMessage(Context ctx, int resId) {
        if (ctx != null) {
            showLongMessage(ctx, ctx.getResources().getString(resId));
        }
    }

    public static void showLongMessage(Context ctx, String msg) {
        // Fix infinite msg displaying
        for (Toast toast : sToasts) {
            toast.cancel();
        }
        sToasts.clear();

        for (int i = 0; i < 3; i++) {
            showMessage(ctx, msg, true);
        }
    }

    public static void showLongMessage(Context ctx, String template, Object... params) {
        // Fix infinite msg displaying
        for (Toast toast : sToasts) {
            toast.cancel();
        }
        sToasts.clear();

        for (int i = 0; i < 3; i++) {
            showMessage(ctx, String.format(template, params), true);
        }
    }

    public static void showLongMessageEndPause(Context context, int resId) {
        showLongMessage(context, resId);

        try {
            Thread.sleep(5_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void cancelToasts() {
        for (Toast toast : sToasts) {
            toast.cancel();
        }
    }

    private static void fixTextSize(Toast toast, Context context) {
        if (sTextSize == 0) {
            // Maintain text size between com.sonique.app rebooting
            sTextSize = context.getResources().getDimension(R.dimen.dialog_text_size);
        }

        TextView messageTextView = extractMessageView(toast);
        messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, sTextSize);
    }

    private static void addAndCancelPrevIfNeeded(Toast newToast, boolean isLong) {
        CharSequence originText = extractText(newToast);

        Helpers.removeIf(sToasts, toast -> {
            // Smart cancel only toasts that have different message
            // So remains possibility to long message to be displayed
            boolean doRemove = !isLong || !extractText(toast).equals(originText);
            if (doRemove) {
                toast.cancel();
            }
            return doRemove;
        });

        sToasts.add(newToast);
    }

    private static CharSequence extractText(Toast toast) {
        TextView messageTextView = extractMessageView(toast);
        return messageTextView.getText();
    }

    private static TextView extractMessageView(Toast toast) {
        ViewGroup group = (ViewGroup) toast.getView();
        return (TextView) group.getChildAt(0);
    }

    private static void setupCleanup() {
        sHandler.removeCallbacks(sCleanupContext);
        sHandler.postDelayed(sCleanupContext, CLEANUP_TIMEOUT_MS);
    }
}
