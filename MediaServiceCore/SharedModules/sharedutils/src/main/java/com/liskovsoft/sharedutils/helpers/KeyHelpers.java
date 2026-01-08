package com.liskovsoft.sharedutils.helpers;

import android.app.Activity;
import android.content.res.Configuration;
import android.view.KeyEvent;
import android.widget.EditText;

public class KeyHelpers {
     
    private static final int KEYCODE_SVC_EXIT = 319;
     
    private static final int KEYCODE_TV_CONTENTS_MENU = 256;
     
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

     
    public static void fixEnterKey(EditText... editFields) {
        if (editFields == null || editFields.length == 0) {
            return;
        }

        for (EditText editField : editFields) {
            editField.setOnKeyListener((v, keyCode, event) -> {
                 
                 
                 
                 

                if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                     
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        Helpers.showKeyboardAlt(v.getContext(), v);
                    }
                    return true;  
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
