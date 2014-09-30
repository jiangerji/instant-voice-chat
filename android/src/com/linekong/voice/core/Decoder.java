package com.linekong.voice.core;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.os.Handler;
import android.util.Log;

import com.linekong.voice.io.FilePlayerConsumer;
import com.linekong.voice.util.ByteBuffer;


public class Decoder implements Runnable {
    private static final String TAG = "Decorder";
    
    private final Object mMutex = new Object();
    private Speex mSpeex = Speex.getInstance();
    private volatile boolean mIsRunning;
    
    private List<ByteBuffer> mByteBuffers = null;
    
    public static int DECODER_BUFFER_SIZE = 160;// 单位为sample
    
    private short[] processedData = null;
    
    private FilePlayerConsumer mConsumer;
    private Thread mConsumerThread;
    
    public Decoder(Handler handler, String audioID, int type) throws FileNotFoundException {
        mByteBuffers = Collections.synchronizedList(new LinkedList<ByteBuffer>());
        
        mConsumer = new FilePlayerConsumer(handler, audioID, type);
        mConsumerThread = new Thread(mConsumer);
        
        DECODER_BUFFER_SIZE = Speex.mDecodeSize;
        processedData = new short[Speex.mEncodeSize/2];
        
        mCacheData = new byte[DECODER_BUFFER_SIZE];
        Log.v(TAG, "Encode "+Speex.mEncodeSize+" "+Speex.mDecodeSize);
    }
    
    @Override
    public void run() {
        android.os.Process
            .setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        int getSize = 0;
        
        // 开始消费者进程
        mConsumer.start();
        mConsumerThread.start();
        Log.v(TAG, "Start FilePlayerConsumer!");
        
        while (isRunning() || (!isIdle())) {
            synchronized (mMutex) {
                while (isIdle()) {
                    try {
                        if (!isRunning()){
                            break;
                        }
                        
                        mMutex.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            synchronized (mMutex) {
                if (mByteBuffers.size() > 0){
                    ByteBuffer db = mByteBuffers.remove(0);
                    
                    getSize = mSpeex.decode(db.mData, processedData, db.mSize);
                    if (getSize > 0) {
                        mConsumer.putData(processedData, getSize);
                    }
                }
            }
        }
        
        Log.v(TAG, "Decode Finish!");
        mConsumer.stop();
    }
    
    
    // 为了保证提供给decode的对象为decode_frame_size的整数倍，将剩余字节进行缓存
    private byte[] mCacheData = null;
    private int mCacheSize = 0;
    
    public void putData(byte[] data, int size){
        if (mCacheSize == 0){
            int count = size/DECODER_BUFFER_SIZE;
            
            for (int i = 0; i < count; i++) {
                ByteBuffer db = new ByteBuffer(DECODER_BUFFER_SIZE);
                synchronized (mMutex) {
                    System.arraycopy(data, DECODER_BUFFER_SIZE*i, db.mData, 0, DECODER_BUFFER_SIZE);
                    
                    mByteBuffers.add(db);
                    mMutex.notify();
                }
            }
            
            // 将剩余的字节进行缓存
            mCacheSize = size%DECODER_BUFFER_SIZE;
            if (mCacheSize > 0){
                System.arraycopy(data, DECODER_BUFFER_SIZE*count, mCacheData, 0, mCacheSize);
            }
        } else {
            if (mCacheSize + size < DECODER_BUFFER_SIZE){
                // 不足一帧数据
                mCacheSize += size;
                System.arraycopy(data, 0, mCacheData, mCacheSize, size);
            } else {
                System.arraycopy(data, 0, mCacheData, mCacheSize, DECODER_BUFFER_SIZE - mCacheSize);
                
                ByteBuffer db = new ByteBuffer(DECODER_BUFFER_SIZE);
                synchronized (mMutex) {
                    System.arraycopy(mCacheData, 0, db.mData, 0, DECODER_BUFFER_SIZE);
                    
                    mByteBuffers.add(db);
                    mMutex.notify();
                }
                
                // 剩余的数据
                size -= (DECODER_BUFFER_SIZE - mCacheSize);
                int offset = DECODER_BUFFER_SIZE - mCacheSize;
                int count = size/DECODER_BUFFER_SIZE;
                
                for (int i = 0; i < count; i++) {
                    db = new ByteBuffer(DECODER_BUFFER_SIZE);
                    synchronized (mMutex) {
                        System.arraycopy(data, offset+DECODER_BUFFER_SIZE*i, db.mData, 0, DECODER_BUFFER_SIZE);
                        
                        mByteBuffers.add(db);
                        mMutex.notify();
                    }
                }
                
                // 将剩余的字节进行缓存
                mCacheSize = size%DECODER_BUFFER_SIZE;
                if (mCacheSize > 0){
                    System.arraycopy(data, offset+DECODER_BUFFER_SIZE*count, mCacheData, 0, mCacheSize);
                }
            }
        }
    }
    
    private boolean isIdle() {
        return mByteBuffers.size() == 0 ? true : false;
    }

    public void start(){
        setRunning(true);
    }
    
    public void stop() {
        // 将缓存中的数据写入consumer
        if (mCacheSize > 0){
            ByteBuffer db = new ByteBuffer(DECODER_BUFFER_SIZE);
            synchronized (mMutex) {
                System.arraycopy(mCacheData, 0, db.mData, 0, mCacheSize);
                
                mByteBuffers.add(db);
                mMutex.notify();
            }
        }
        mCacheSize = 0;
        
        setRunning(false);
    }
    
    public void stopPlay(){
        setRunning(false);
        mConsumer.stopPlay();
    }
    
    private void setRunning(boolean isRunning) {
        synchronized (mMutex) {
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
