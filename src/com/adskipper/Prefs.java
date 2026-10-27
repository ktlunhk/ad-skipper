package com.adskipper;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {

    private static final String FILE = "adskipper";
    private static final String KEY_AUTO_SKIP = "auto_skip";
    private static final String KEY_MUTE = "mute_during_ads";

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static boolean isAutoSkip(Context c) {
        return sp(c).getBoolean(KEY_AUTO_SKIP, true);
    }

    public static void setAutoSkip(Context c, boolean value) {
        sp(c).edit().putBoolean(KEY_AUTO_SKIP, value).apply();
    }

    public static boolean isMute(Context c) {
        return sp(c).getBoolean(KEY_MUTE, true);
    }

    public static void setMute(Context c, boolean value) {
        sp(c).edit().putBoolean(KEY_MUTE, value).apply();
    }
}
