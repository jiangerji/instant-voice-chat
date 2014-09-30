package com.linekong.voice.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.linekong.voice.ogg.SpeexDecoder;
import com.linekong.voice.util.CacheFile;
import com.linekong.voice.util.Params;

public class PlayerClient implements Runnable {
    private final static String TAG = "PlayerClient";
    private final Object mutex = new Object();
    private volatile boolean mIsRunning;

    private String mAudioID;
    private Handler mHandler;
    private int mType = 0;

    private SpeexDecoder speexdec = null;

    public PlayerClient(Context context, Handler handler, String audioID,
            int type) {
        mAudioID = audioID;
        mHandler = handler;
        mType = type;
    }

    @Override
    public void run() {
        FileInputStream mCache = null;

        mCache = CacheFile.openCacheReadStream(mAudioID);

        if (mCache == null) {
            Log.v(TAG, "Not found cache file, download from server!");
            mHandler.sendEmptyMessage(Params.PLAY_BY_NET);

            int response = CacheFile.doDownload(mAudioID, mType);

            Message msg = mHandler.obtainMessage(Params.DOWNLOAD_FINISH);
            msg.arg1 = response;
            msg.obj = mAudioID;
            mHandler.sendMessage(msg);

            if (response == CacheFile.DOWNLOAD_SUCCESS) {
                mCache = CacheFile.openCacheReadStream(mAudioID);
            } else {
                Message amsg = mHandler.obtainMessage(Params.PLAY_FINISH);
                amsg.obj = mAudioID;
                mHandler.sendMessage(amsg);

                return;
            }
        }

        if (mCache != null) {
            try {
                mCache.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mCache == null || (!isRunning())) {
            Message amsg = mHandler.obtainMessage(Params.PLAY_FINISH);
            amsg.obj = mAudioID;
            mHandler.sendMessage(amsg);

            return;
        }

        Log.v(TAG, "Play the cache file!");
        mHandler.sendEmptyMessage(Params.PLAY_BY_FILE);

        try {
            speexdec = new SpeexDecoder(new File(
                    CacheFile.getCacheFilePath(mAudioID, mType)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        mHandler.sendEmptyMessage(Params.PLAY_START);
        try {
            if (speexdec != null) {
                speexdec.decode();
            }
        } catch (Exception t) {
            t.printStackTrace();
        }

        Message msg = mHandler.obtainMessage(Params.PLAY_FINISH);
        msg.obj = mAudioID;
        mHandler.sendMessage(msg);

        Log.v(TAG, "Finish!");
    }

    public void start() {
        setRunning(true);
    }

    public void stop() {
        setRunning(false);
        if (speexdec != null) {
            speexdec.setPaused(true);
        }
    }

    public void stopPlay() {
        stop();
        //        if (decoder != null){
        //            decoder.stopPlay();
        //        }
    }

    private void setRunning(boolean isRunning) {
        synchronized (mutex) {
            mIsRunning = isRunning;
            mutex.notify();
        }
    }

    private boolean isRunning() {
        synchronized (mutex) {
            return mIsRunning;
        }
    }

}
