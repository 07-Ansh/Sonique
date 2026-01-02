package com.liskovsoft.sharedutils.helpers;

import android.app.Activity;
import android.content.res.Configuration;
import android.view.KeyEvent;
import android.widget.EditText;

public class KeyHelpers {
    // Philips ambilight button
    private static final int KEYCODE_SVC_EXIT = 319;
    /** Key code constant: Contents menu key.
     * Goes to the title list. Corresponds to Contents Menu (0x0B) of CEC User Control
     * Code */
    private static final int KEYCODE_TV_CONTENTS_MENU = 256;
    /** Key code constant: Media context menu key.
     * Goes to the context menu of media contents. Corresponds to Media Context-sensitive
     * Menu (0x11) of CEC User Control Code. */
    private static final int KEYCODE_TV_MEDIA_CONTEXT_MENU = 257;

    public static void press(Activity activity, int keyCode) {
        KeyEvent newEventDown = newEvent(KeyEvent.ACTION_DOWN, keyCode);
        KeyEvent newEventUp = newEvent(KeyEvent.ACTION_UP, keyCode);

        activity.dispatchKeyEvent(newEventDown);
        activity.dispatchKeyEvent(newEventUp);
    }

    public static KeyEvent newEvent(KeyEvent origin, int newKeyCode) {
        return new KeyEvent(
                origin.getDownTime(),
                origin.getEventTime(),
                origin.getAction(),
                newKeyCode,
                origin.getRepeatCount(),
                origin.getMetaState(),
                origin.getDeviceId(),
                origin.getScanCode(),
                origin.getFlags(),
                origin.getSource());
    }

    public static KeyEvent newEvent(int action, int keyCode) {
        return new KeyEvent(action, keyCode);
    }

    /** Whether the key will, by default, trigger a click on the focused view.
     */
    public static boolean isConfirmKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_BUTTON_A:
                return true;
            default:
                return false;
        }
    }

    /**
     * Whether this key is a media key, which can be send to apps that are
     * interested in media key events.
     *
     * @hide
     */
    public static final boolean isMediaKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MUTE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_STOP:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_MEDIA_RECORD:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                return true;
        }
        return false;
    }

    /**
     * Philips ambilight button
     */
    public static boolean isAmbilightKey(int keyCode) {
        return keyCode == KEYCODE_SVC_EXIT;
    }

    public static boolean isBackKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_BACK ||
               keyCode == KeyEvent.KEYCODE_ESCAPE;
    }

    public static boolean isMenuKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_MENU ||
               keyCode == KEYCODE_TV_CONTENTS_MENU ||
               keyCode == KEYCODE_TV_MEDIA_CONTEXT_MENU ||
               keyCode == KeyEvent.KEYCODE_INFO;
    }

    public static boolean isStopKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_MEDIA_STOP;
    }

    public static boolean isTogglePlaybackKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
    }

    public static boolean isLeftRightKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT;
    }

    public static boolean isUpDownKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN;
    }

    public static boolean isNavigationKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN;
    }

    /**
     * G20s fix (Enter mapped to OK): show soft keyboard on textview click<br/>
     * More info: https://stackoverflow.com/questions/1489852/android-handle-enter-in-an-edittext
     */
    public static void fixEnterKey(EditText... editFields) {
        if (editFields == null || editFields.length == 0) {
            return;
        }

        for (EditText editField : editFields) {
            editField.setOnKeyListener((v, keyCode, event) -> {
                // Skip physical keyboard. Cause bugs. Soft keyboard won't pop in.
                //if (v.getResources().getConfiguration().keyboard == Configuration.KEYBOARD_QWERTY) {
                //    return false;
                //}

                if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                    // Perform action on key press
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        Helpers.showKeyboardAlt(v.getContext(), v);
                    }
                    return true; // disable default action (text auto commit)
                }
                return false;
            });
        }
    }

    public static void fixShowKeyboard(EditText... editFields) {
        if (editFields == null || editFields.length == 0) {
            return;
        }

        for (EditText editField : editFields) {
            if (editField.getOnFocusChangeListener() != null) {
                continue;
            }

            editField.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    Helpers.showKeyboardAlt(v.getContext(), v);
                }
            });
        }
    }
}
