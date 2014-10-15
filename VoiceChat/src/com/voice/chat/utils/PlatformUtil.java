package com.voice.chat.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.Log;

import com.voice.chat.LKApplication;
import com.voice.chat.service.ShortCutService;
import com.voice.chat.ui.ShortCutActivity;

public class PlatformUtil {

    private final static String TAG = "voice";

    /**
     * 保存值到share preference中
     */
    public static void setPreferenceValue(String key, int value) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(LKApplication.getApplication());
        Editor editor = settings.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    /**
     * 获取share preference中保存的值
     */
    public static long getPreferenceValue(String key, long defaultValue) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(LKApplication.getApplication());
        return settings.getLong(key, defaultValue);
    }

    /**
     * 保存值到share preference中
     */
    public static void setPreferenceValue(String key, long value) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(LKApplication.getApplication());
        Editor editor = settings.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    /**
     * 获取share preference中保存的值
     */
    public static String getPreferenceValue(String key, String defaultValue) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(LKApplication.getApplication());
        return settings.getString(key, defaultValue);
    }

    /**
     * 保存值到share preference中
     */
    public static void setPreferenceValue(String key, String value) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(LKApplication.getApplication());
        Editor editor = settings.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * 获取share preference中保存的值
     */
    public static boolean getPreferenceValue(String key, boolean defaultValue) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(LKApplication.getApplication());
        return settings.getBoolean(key, defaultValue);
    }

    /**
     * 保存值到share preference中
     */
    public static void setPreferenceValue(String key, boolean value) {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(LKApplication.getApplication());
        Editor editor = settings.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public static void addShortCut(Context context, String packageName) {
        Log.d(TAG, "addShortCut: " + packageName);
        Intent shortcutIntent = new Intent(context, ShortCutActivity.class);

        shortcutIntent.putExtra(ShortCutService.GAME_PACAGE_NAME, packageName);
        shortcutIntent.setAction(Intent.ACTION_DEFAULT);

        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);

            Intent addIntent = new Intent();
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, info.loadLabel(pm)
                    + "YY");
            Drawable iconDrawable = info.loadIcon(pm);
            BitmapDrawable bd = (BitmapDrawable) iconDrawable;
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bd.getBitmap());

            addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            context.sendBroadcast(addIntent);
        } catch (Exception e) {

        }

    }
}
