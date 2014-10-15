package com.voice.chat.service;

import java.util.List;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class WatchService extends Service {

    private final static String TAG = "voice";

    private static final String ACTION_START = "com.voice.test.keepalive.START";
    private static final String ACTION_STOP = "com.voice.test.keepalive.STOP";

    private boolean mStarted;

    public static void actionStart(Context ctx) {
        Intent i = new Intent(ctx, WatchService.class);
        i.setAction(ACTION_START);
        ctx.startService(i);
    }

    public static void actionStop(Context ctx) {
        Intent i = new Intent(ctx, WatchService.class);
        i.setAction(ACTION_STOP);
        ctx.startService(i);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent == null || intent.getAction() == null) {
            start();
            return START_STICKY;
        }

        String action = intent.getAction();

        if (action.equals(ACTION_STOP) == true) {
            stop();
            stopSelf();
        } else if (action.equals(ACTION_START) == true) {
            start();
        }

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private synchronized void start() {
        if (mStarted == true) {
            Log.w(TAG, "Attempt to start connection that is already active");
            return;
        }

        setStarted(true);

        mThread = new CheckRunningActivity(this);
        mThread.start();
    }

    private synchronized void stop() {
        if (mStarted == false) {
            Log.w(TAG, "Attempt to stop connection not active.");
            return;
        }

        setStarted(false);

        if (mThread != null) {
            mThread.abort();
            mThread = null;
        }
    }

    private void setStarted(boolean started) {
        mStarted = started;
    }

    private CheckRunningActivity mThread = null;

    class CheckRunningActivity extends Thread {
        ActivityManager am = null;
        Context context = null;
        private volatile boolean mRunning = false;

        public CheckRunningActivity(Context con) {
            context = con;
            am = (ActivityManager) context
                    .getSystemService(Context.ACTIVITY_SERVICE);
            mRunning = true;
        }

        public void run() {

            Log.d(TAG, "check thread start");
            while (mRunning) {
                // Return a list of the tasks that are currently running,
                // with the most recent being first and older ones after in
                // order.
                // Taken 1 inside getRunningTasks method means want to take only
                // top activity from stack and forgot the olders.
                List<ActivityManager.RunningTaskInfo> taskInfo = am
                        .getRunningTasks(1);

                String currentRunningActivityName = taskInfo.get(0).topActivity
                        .getClassName();

                Log.d(TAG, "currentRunningActivityName "
                        + currentRunningActivityName);
                if (currentRunningActivityName
                        .equals("PACKAGE_NAME.ACTIVITY_NAME")) {

                }

                try {
                    sleep(2000);
                } catch (InterruptedException e) {

                }
            }
            Log.d(TAG, "check thread stop");
        }

        public void abort() {
            mRunning = false;
            interrupt();
        }
    }
}
