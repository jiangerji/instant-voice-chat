package com.voice.chat;

import android.app.Application;

public class LKApplication extends Application {

    private static LKApplication mApplication;

    public static LKApplication getApplication() {
        return mApplication;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mApplication = this;
    }
}
