package com.crazy.x.audio;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.util.Log;

import com.linekong.voice.core.Speex;
import com.linekong.voice.util.DataBuffer;

public class XEncoder extends Thread {
    private Speex mSpeex = Speex.getInstance();

    private volatile boolean mIsRunning;
    private final Object mMutex = new Object();

    public static int ENCODER_BUFFER_SIZE = 320;

    private byte[] mProcessedData = null;

    private List<DataBuffer> mDataBuffers = null;

    private static final String TAG = "Encoder";

    public static interface XEncoderListener {
        public void onEncoderStart();

        public void onEncoderContent(byte[] content, int length);

        public void onEncoderStop();
    }

    private XEncoderListener mListener = null;

    public XEncoder(XEncoderListener listener) {
        mDataBuffers = Collections.synchronizedList(new LinkedList<DataBuffer>());

        ENCODER_BUFFER_SIZE = mSpeex.getEncodeFrameSize();
        mProcessedData = new byte[ENCODER_BUFFER_SIZE * 2];
        mCacheData = new short[ENCODER_BUFFER_SIZE];

        Speex.mEncodeSize = ENCODER_BUFFER_SIZE * 2;
        Log.v(TAG, "Speex Frame Size:" + ENCODER_BUFFER_SIZE + "samples!");

        mListener = listener;
    }

    @Override
    public void run() {
        android.os.Process
                .setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        int getSize = 0;

        if (mListener != null) {
            mListener.onEncoderStart();
        }

        Log.v(TAG, "Encoder start to encode!");
        while (isRunning() || (!isIdle())) {
            synchronized (mMutex) {
                while (isIdle()) {
                    try {
                        // 所有的数据都已经处理完毕，退出
                        if (!isRunning()) {
                            break;
                        }

                        mMutex.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            synchronized (mMutex) {
                if (mDataBuffers.size() > 0) {
                    DataBuffer db = mDataBuffers.remove(0);

                    getSize = mSpeex.encode(db.mData,
                            0,
                            mProcessedData,
                            db.mSize);
                    Speex.mDecodeSize = getSize;
                    // TODO: 更新压缩比例，目前是写死为16，会压缩16倍
                    if (getSize > 0) {
                        if (mListener != null) {
                            mListener.onEncoderContent(mProcessedData, getSize);
                        }
                    }
                }
            }
        }

        Log.v(TAG, "Encoder finish encoder!");
        if (mListener != null) {
            mListener.onEncoderStop();
        }
    }

    // 为了保证提供给encode的对象为encode_frame_size的整数倍，将剩余字节进行缓存
    private short[] mCacheData = null;
    private int mCacheSize = 0;

    /**
     * 放入语音数据进行处理
     * 
     * @param data
     *            语音数据buffer
     * @param size
     *            语音数据buffer长度
     */
    public void putData(short[] data, int size) {
        if (mCacheSize == 0) {
            int count = size / ENCODER_BUFFER_SIZE;

            for (int i = 0; i < count; i++) {
                DataBuffer db = new DataBuffer(ENCODER_BUFFER_SIZE);
                synchronized (mMutex) {
                    System.arraycopy(data,
                            ENCODER_BUFFER_SIZE * i,
                            db.mData,
                            0,
                            ENCODER_BUFFER_SIZE);

                    mDataBuffers.add(db);
                    mMutex.notify();
                }
            }

            // 将剩余的字节进行缓存
            mCacheSize = size % ENCODER_BUFFER_SIZE;
            if (mCacheSize > 0) {
                System.arraycopy(data,
                        ENCODER_BUFFER_SIZE * count,
                        mCacheData,
                        0,
                        mCacheSize);
            }
        } else {
            if (mCacheSize + size < ENCODER_BUFFER_SIZE) {
                // 不足一帧数据
                mCacheSize += size;
                System.arraycopy(data, 0, mCacheData, mCacheSize, size);
            } else {
                System.arraycopy(data, 0, mCacheData, mCacheSize,
                        ENCODER_BUFFER_SIZE - mCacheSize);

                DataBuffer db = new DataBuffer(ENCODER_BUFFER_SIZE);
                synchronized (mMutex) {
                    System.arraycopy(mCacheData, 0, db.mData, 0,
                            ENCODER_BUFFER_SIZE);

                    mDataBuffers.add(db);
                    mMutex.notify();
                }

                // 剩余的数据
                size -= (ENCODER_BUFFER_SIZE - mCacheSize);
                int offset = ENCODER_BUFFER_SIZE - mCacheSize;
                int count = size / ENCODER_BUFFER_SIZE;

                for (int i = 0; i < count; i++) {
                    db = new DataBuffer(ENCODER_BUFFER_SIZE);
                    synchronized (mMutex) {
                        System.arraycopy(data,
                                offset + ENCODER_BUFFER_SIZE * i,
                                db.mData,
                                0,
                                ENCODER_BUFFER_SIZE);

                        mDataBuffers.add(db);
                        mMutex.notify();
                    }
                }

                // 将剩余的字节进行缓存
                mCacheSize = size % ENCODER_BUFFER_SIZE;
                if (mCacheSize > 0) {
                    System.arraycopy(data,
                            offset + ENCODER_BUFFER_SIZE * count,
                            mCacheData,
                            0,
                            mCacheSize);
                }
            }
        }
    }

    /**
     * 是否还有数据需要处理
     * 
     * @return
     */
    private boolean isIdle() {
        return mDataBuffers.size() == 0 ? true : false;
    }

    /**
     * 启动编码线程
     */
    public void startEncoder() {
        setRunning(true);
        start();
    }

    /**
     * 停止编码线程
     */
    public void stopEncoder() {
        if (mCacheSize > 0) {
            DataBuffer db = new DataBuffer(ENCODER_BUFFER_SIZE);
            synchronized (mMutex) {
                System.arraycopy(mCacheData, 0, db.mData, 0, mCacheSize);

                mDataBuffers.add(db);
                mMutex.notify();
            }
        }
        mCacheSize = 0;
        setRunning(false);
    }

    /**
     * 设置线程是否需要继续进行，当设置为false时，会将未处理完成的数据，处理完毕后退出线程
     * 
     * @param isRunning
     */
    private void setRunning(boolean isRunning) {
        synchronized (mMutex) {
            Log.v(TAG, "Encoder: set running to " + isRunning);
            mIsRunning = isRunning;
            mMutex.notify();
        }
    }

    public boolean isRunning() {
        synchronized (mMutex) {
            return mIsRunning;
        }
    }
}
