package com.linekong.voice;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Activity;
import android.util.Log;

import com.linekong.util.CommonUtils;
import com.linekong.voice.VoiceManager.PlayerListener;
import com.linekong.voice.VoiceManager.RecordListener;

public class UnityWrapper {

    private static final String TAG = "UnityWrapper";
    VoiceManager mVoiceManager = null;

    public UnityWrapper() {

    }

    private Method mSendMessage = null;

    /**
     * 获取当前unity的activity实例
     * @return
     */
    private Activity getUnityCurrentActivity() {
        Activity activity = null;

        try {
            Class<?> unityPlayer = Class.forName("com.unity3d.player.UnityPlayer");
            Field currentActivity = unityPlayer.getField("currentActivity");
            mSendMessage = unityPlayer.getMethod("UnitySendMessage", String.class, String.class,
                    String.class);
            activity = (Activity) currentActivity.get(null);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new IllegalStateException("not in unity environment!");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            throw new IllegalStateException("Unity is not offical!");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new IllegalStateException(
                    "Unity does not have UnitySendMessage method!");
        }

        if (activity == null) {
            throw new IllegalStateException(
                    "Please call init function after unity has been launched!");
        }

        return activity;
    }

    private void sendMessage(String target, String info) {
        if (mSendMessage != null) {
            Log.d(TAG, "UnitySendMessage:" + target + ", " + info);
            try {
                mSendMessage.invoke(null, mUnityObjName, target, info);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    public void init(String gameId, String userId, String serverUrl) {
        Log.v(TAG, "init");
        VoiceManager.init(getUnityCurrentActivity());
        mVoiceManager = VoiceManager.getInstance();
        setGameID(gameId);
        setUserID(userId);
        setServerUrl(serverUrl);
    }

    public void deinit() {
        Log.v(TAG, "deinit");
        VoiceManager.deinit();
        mVoiceManager = null;
    }

    private String mUnityObjName;

    public void setUnityObjName(String name) {
        mUnityObjName = name;
        Log.v(TAG, "setUnityObjName");
    }

    public void setGameID(String gameID) {
        mVoiceManager.setGameID(gameID);
        Log.v(TAG, "setGameID");
    }

    public void setUserID(String uid) {
        mVoiceManager.setUserID(uid);
        Log.v(TAG, "setUserID");
    }

    public void setServerUrl(String url) {
        mVoiceManager.setServerUrl(url);
    }

    RecordListener mRecordListener = new RecordListener() {

        @Override
        public void onUploadFinish(String recordID, int status) {
            Log.v(TAG, "onUploadFinish:" + recordID + ":" + status);
            sendMessage("onUploadFinish", recordID + ":" + status);
        }

        @Override
        public void onRecordStart() {
            Log.v(TAG, "onRecordStart");
            sendMessage("onRecordStart", "");
        }

        @Override
        public void onRecordFinish(String recordID, int duration) {
            Log.v(TAG, "onRecordFinish:" + recordID + ":" + duration);
            sendMessage("onRecordFinish", recordID + ":" + duration);
        }
    };

    public void startRecord(int type) {
        Log.v(TAG, "startRecord");
        mVoiceManager.startRecord(mRecordListener, type);
    }

    public void stopRecord() {
        Log.v(TAG, "stopRecord");
        mVoiceManager.stopRecord();
    }

    PlayerListener mPlayerListener = new PlayerListener() {

        @Override
        public void onPlayerStatus(String recordID, int status) {
            sendMessage("onPlayerStatus", recordID + ":" + status);
        }

        @Override
        public void onPlayerStart() {
            sendMessage("onPlayerStart", "");
        }

        @Override
        public void onPlayerFinish(String recordID) {
            sendMessage("onPlayerFinish", recordID);
        }
    };

    public void startPlay(String recordID, int type) {
        Log.v(TAG, "startPlay");
        mVoiceManager.startPlay(recordID, type, mPlayerListener);
    }

    public void stopPlay() {
        Log.v(TAG, "stopPlay");
        mVoiceManager.stopPlay();
    }

    @SuppressWarnings("deprecation")
    public void uploadRecord(String recordID) {
        Log.v(TAG, "startPlay");
        mVoiceManager.uploadRecord(recordID);
    }

    public void clearRecord(String audioId, int type) {
        mVoiceManager.clearRecord(audioId, type);
    }

    public void clearCache() {
        mVoiceManager.clearCache();
    }

    public void showWebView(String url) {
        CommonUtils.showWebView(getUnityCurrentActivity(), url);
    }

    public void showWebView(String url, int width, int height) {
        CommonUtils.showWebView(getUnityCurrentActivity(), url, width, height);
    }

    public void actionStatistic(int type, String gameId, String uid, String desc) {
        CommonUtils.actionStatistic(getUnityCurrentActivity(), type, gameId, uid, desc);
    }
}
