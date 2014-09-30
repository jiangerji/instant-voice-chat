package com.linekong.voice;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.linekong.voice.core.Speex;
import com.linekong.voice.io.PcmRecorder;
import com.linekong.voice.io.PlayerClient;
import com.linekong.voice.util.CacheFile;
import com.linekong.voice.util.Params;

public class VoiceManager {
    private final static String TAG = "VoiceManager";

    private static VoiceManager _instance = null;
    private Handler mHandler = null;

    private VoiceManager(Context context) {
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                Looper.prepare();

                mHandler = new Handler(new Callback() {

                    @Override
                    public boolean handleMessage(Message msg) {
                        Log.v(TAG, "Recv handle message!" + msg);

                        if (msg == null) {
                            return false;
                        }

                        switch (msg.what) {
                        case Params.PLAY_START:
                            if (mPlayerListener != null) {
                                mPlayerListener.onPlayerStart();
                            }
                            break;

                        case Params.PLAY_FINISH:
                            if (mNeedPlay) {
                                mCurrentPlayer.start();
                                Thread thread = new Thread(mCurrentPlayer);
                                thread.start();
                                mNeedPlay = false;
                            } else {
                                if (mCurrentPlayer != null) {
                                    mCurrentPlayer = null;
                                }
                            }

                            mPlayerListener.onPlayerFinish((String) msg.obj);
                            break;

                        case Params.RECORD_START:
                            if (mRecordListener != null) {
                                mRecordListener.onRecordStart();
                            }
                            break;

                        case Params.RECORD_FINISH:
                            if (mCurrentRecorder != null) {
                                mCurrentRecorder = null;
                            }

                            if (mRecordListener != null) {
                                if (msg.obj == null) {
                                    mRecordListener.onRecordFinish("", -1);
                                } else {
                                    mRecordListener.onRecordFinish((String) msg.obj, msg.arg1);
                                }
                            }
                            break;

                        case Params.UPLOAD_FINISH:
                            if (mRecordListener != null) {
                                mRecordListener.onUploadFinish((String) msg.obj, msg.arg1);
                            }
                            break;

                        case Params.DOWNLOAD_FINISH:
                            if (mPlayerListener != null) {
                                mPlayerListener.onPlayerStatus((String) msg.obj, msg.arg1);
                            }
                        }
                        return false;
                    }
                });

                Looper.loop();
                Log.v(TAG, "Loop thread exit!");
            }
        });

        thread.start();
    }

    private Context mContext = null;

    /**
     * 初始化VoiceManager对象，请最先调用该接口
     * @param context   android context对象
     */
    public static synchronized void init(Context context) {
        if (_instance == null) {
            _instance = new VoiceManager(context);
        }

        _instance.mContext = context;

        Speex.init(0);
        CacheFile.init();
        Log.v(TAG, "VoiceManager Init Finished!");
    }

    /**
     * 请不要调用这个接口，否则后果自负
     * @param level
     */
    @SuppressWarnings("unused")
    private void setLevel(int level) {
        Speex.setLevel(level);
    }

    /**
     * 设置game id, 由蓝港为该游戏分配
     * 请在调用语聊其他接口前，调用该接口进行设置，否则无法使用语聊功能
     * @param gameID    蓝港分配给该游戏的game id
     */
    public void setGameID(String gameID) {
        // TODO: 进行GameID验证，是否有效
        Params.mGameID = gameID;
    }

    /**
     * 设置当前游戏用户ID
     * @param uid   游戏客户端提供，用于唯一标识使用语音的用户。
     */
    public void setUserID(String uid) {
        // TODO: 进行UserID验证，是否有效
        Params.mUserID = uid;
    }

    /**
     * 设置语聊服务器的地址，如果没有，该值请传null,会使用蓝港自己的语聊服务器
     * @param url   语聊服务器地址，如果没有请传null
     */
    public void setServerUrl(String url) {
        if (url != null && url.length() > 0) {
            Params.mServerUrl = url;
        }
    }

    /**
     * 销毁VoiceManager对象，删除临时语音文件缓存，并释放相关资源
     */
    public static synchronized void deinit() {
        _instance.stopPlay();
        _instance.stopRecord();

        // TODO: 可能会crash
        Speex.deinit();

        _instance.mHandler.getLooper().quit();

        _instance.clearTmpCache();//删除播放残余文件
        _instance = null;
        Log.v(TAG, "VoiceManager deinit Finished!");
    }

    /**
     * 请一定要在init之后调用这个接口
     * @return  返回VoiceManager实例
     */
    public static synchronized VoiceManager getInstance() {
        return _instance;
    }

    /**
     * 获取当前语聊的context
     * @return  当前语聊使用的context
     */
    public Context getContext() {
        return mContext;
    }

    private PcmRecorder mCurrentRecorder = null;
    private RecordListener mRecordListener = null;

    public interface RecordListener {
        /**
         * 录音开始回调该接口
         */
        void onRecordStart();

        /**
         * 录音结束回调接口，录音的最长时长会限制在30S
         * @param recordID  此次录音的id，客户端需要播放或删除等操作都需要使用该id
         * @param duration  此次录音的大概时长，单位为秒
         */
        void onRecordFinish(String recordID, int duration);

        /**
         * 录音结束后，会将语音上传到语聊服务器，上传结束后，会回调该接口
         * @param recordID  此次上传的录音id
         * @param status    上传状态，0表示成功，其他表示失败
         */
        void onUploadFinish(String recordID, int status);
    }

    private Timer mTimeoutTimer;

    /**
     * 开始录音，如果当前有正在播放语聊文件，会停止当前播放的语聊文件
     * @param recordListener    监听录音状态listener
     * @param type              此次录音的类型，
     *                          0表示临时录音，临时录音在销毁语聊SDK后在客户端缓存中删除
     *                          1表示永久录音，永久录音语聊SDK不会主动删除缓存
     *                          所有语聊都会永久保存在服务器上
     */
    public void startRecord(RecordListener recordListener, int type) {
        stopPlay();

        if (mCurrentRecorder == null) {
            PcmRecorder pcmRecorder = new PcmRecorder(mHandler, type);
            pcmRecorder.start();
            Thread thread = new Thread(pcmRecorder);
            thread.start();

            mCurrentRecorder = pcmRecorder;
            mRecordListener = recordListener;

            if (mTimeoutTimer != null) {
                mTimeoutTimer.cancel();
            }
            mTimeoutTimer = new Timer();
            mTimeoutTimer.schedule(new TimerTask() {

                @Override
                public void run() {
                    if (_instance != null) {
                        _instance.stopRecord();
                        mTimeoutTimer = null;
                    }
                }
            }, 30 * 1000);
            Log.v(TAG, "startRecord:" + recordListener + " " + mHandler);
        } else {
            Log.v(TAG, "In Recording!");
        }
    }

    /**
     * 停止录音当前录音，并上传录音文件
     */
    public void stopRecord() {
        if (mCurrentRecorder != null) {
            Log.v(TAG, "Stop Recording!");
            mCurrentRecorder.stop();
        }
    }

    private PlayerClient mCurrentPlayer = null;
    private PlayerListener mPlayerListener = null;

    public interface PlayerListener {
        /**
         * 当播放的语聊不在本地缓存中，会从网络上下载该录音文件，当下载结束后会回调该接口。
         * 如果录音id在本地缓存中，不会回调到该接口
         * @param recordID  当前播放的录音id
         * @param status    0表示下载成功，其他表示下载失败
         */
        void onPlayerStatus(String recordID, int status);

        /**
         * 开始播放录音文件
         */
        void onPlayerStart();

        /**
         * 播放录音结束
         * @param recordID  此次播放录音id
         */
        void onPlayerFinish(String recordID);
    }

    boolean mNeedPlay = false;

    /**
     * 开始播放某个语音，如果有前一个语聊正在播放，会被停止
     * @param recordID  需要播放的语音记录id
     * @param type      该段语音的类型，0表示临时录音，在退出语音聊天的缓存时会被删除，
     *                  其他值表示永久录音，语音SDK不会主动删除该文件的缓存
     * @param playerListener    播放状态监听对象
     */
    public void startPlay(String recordID, int type,
            PlayerListener playerListener) {
        if (mCurrentPlayer != null) {
            // 停止前一个player的播放
            mCurrentPlayer.stopPlay();
            mCurrentPlayer = new PlayerClient(mContext, mHandler, recordID,
                    type);
            mNeedPlay = true;

            Log.v(TAG, "wait to start play!" + recordID);
        } else {
            mCurrentPlayer = new PlayerClient(mContext, mHandler, recordID,
                    type);
            mCurrentPlayer.start();
            Thread thread = new Thread(mCurrentPlayer);
            thread.start();

            Log.v(TAG, "start play!" + recordID);
        }

        mPlayerListener = playerListener;
    }

    /**
     * 停止播放当前的语音
     */
    public void stopPlay() {
        Log.v(TAG, "stop play!" + mCurrentPlayer);
        if (mCurrentPlayer != null) {
            mCurrentPlayer.stopPlay();
        }
    }

    /**
     * 重新上传某个语音记录
     * @param listener  监听上传状态，会在onUploadFinish接口回调
     * @param recordID  录音id
     */
    public void uploadRecord(RecordListener listener, String recordID) {
        if (listener != null) {
            mRecordListener = listener;
        }

        uploadRecord(recordID);
    }

    /**
     * 重新上传某个语音记录, 如果缓存中该文件已经被删除，这不会上传任何东西，请不要使用该接口
     * @param recordID  语录音id
     */
    @Deprecated
    public void uploadRecord(final String recordID) {
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                int response = CacheFile.doUpload(recordID);
                Message message = mHandler.obtainMessage(Params.UPLOAD_FINISH);
                message.arg1 = response;
                message.obj = recordID;
                mHandler.sendMessage(message);
            }
        });
        thread.start();
    }

    /**
     * 删除所有的缓存的语音文件
     */
    public void clearCache() {
        CacheFile.clearCache();
    }

    /**
     * 删除指定的语音文件
     * @param audioId   语音的id
     * @param type      语音的类型
     */
    public void clearRecord(String audioId, int type) {
        CacheFile.clearRecord(audioId, type);
    }

    /**
     * 删除不需要持久保存的临时语音文件
     */
    public void clearTmpCache() {
        CacheFile.clearTmpCache();
    }
}
