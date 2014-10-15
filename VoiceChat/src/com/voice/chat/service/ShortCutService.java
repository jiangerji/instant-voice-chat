package com.voice.chat.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import com.voice.chat.floating.FloatWindow;
import com.voice.chat.floating.FloatWindowService;

public class ShortCutService extends IntentService {
    private final static String TAG = "voice";

    public static final String GAME_PACAGE_NAME = "game.package.name";
    public static final String GAME_MAIN_ACTIVITY = "game.main.activity";

    public ShortCutService() {
        super("ShortCutService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String pacakgeName = intent.getStringExtra(GAME_PACAGE_NAME);

        Log.d(TAG, "pacakgeName = " + pacakgeName);

        PackageManager packageManager = getPackageManager();
        Intent startintent = packageManager
                .getLaunchIntentForPackage(pacakgeName);
        startintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startintent);

        try {
            Thread.sleep(1000);
        } catch (Exception e) {

        } finally {
            Log.d(TAG, "ShortCutService show Float Window");
            FloatWindowService.show(ShortCutService.this, FloatWindow.class,
                    FloatWindowService.DEFAULT_ID);
        }
    }

}
