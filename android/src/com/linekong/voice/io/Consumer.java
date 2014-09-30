package com.linekong.voice.io;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import android.util.Log;

import com.linekong.voice.util.ByteBuffer;

public abstract class Consumer implements Runnable {
    private List<ByteBuffer> mByteBuffers = null;
    private final Object mMutex = new Object();
    private volatile boolean mIsRunning = true;
    
    public Consumer(){
        mByteBuffers = Collections.synchronizedList(new LinkedList<ByteBuffer>());
    }
    
    public abstract String getTag();
    
    @Override
    public void run() {
        Log.v(getTag(), "start ");
        while (isRunning() || (!isIdle())) {
            synchronized (mMutex) {
                while (isIdle()) {
                    try {
                        // 所有的数据都已经处理完毕，退出
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
                    ByteBuffer byteBuffer = mByteBuffers.remove(0);
                    doProcess(byteBuffer);
                }
            }
        }
        
        Log.v(getTag(), "doFinish ");
        doFinish();
    }

    /**
     * 
     * @param dataBuffer
     * @return
     */
    public abstract int doProcess(ByteBuffer byteBuffer);
    
    /**
     * 
     */
    public abstract void doFinish();
    
    /**
     * 
     * @param data
     * @param size
     */
    public void putData(byte[] data, int size){
        ByteBuffer dataBuffer = new ByteBuffer(size);
        
        synchronized (mMutex) {
            System.arraycopy(data, 0, dataBuffer.mData, 0, size);
            
            mByteBuffers.add(dataBuffer);
            mMutex.notify();
        }
    }
    
    public void putData(short[] data, int size) {
        byte[] byteDate = new byte[size*2];
        
        for (int i = 0; i < size; i++) {
            byteDate[2*i+1] = (byte) ((data[i] & 0xFF00) >> 8);
            byteDate[2*i] = (byte) (data[i] & 0xFF);
        }
        
        putData(byteDate, size*2);
    }
    
    private boolean isIdle() {
        return mByteBuffers.size() == 0 ? true : false;
    }

    /**
     * 
     */
    public final void start() {
        setRunning(true);
    }
    
    /**
     * 
     */
    public final void stop() {
        setRunning(false);
    }
    
    private void setRunning(boolean isRunning) {
        Log.v(getTag(), "setRunning "+isRunning);
        synchronized (mMutex) {
            this.mIsRunning = isRunning;
            mMutex.notify();
        }
    }

    public final boolean isRunning() {
        synchronized (mMutex) {
            return mIsRunning;
        }
    }
}
