package com.linekong.voice.io;

import java.io.FileOutputStream;
import java.io.IOException;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.linekong.voice.core.Encoder;
import com.linekong.voice.core.Speex;
import com.linekong.voice.util.CacheFile;
import com.linekong.voice.util.Params;

/**
 * 
 * @author lk
 *
 */
public class PcmRecorder implements Runnable {
    private volatile boolean isRunning = true;
    private final Object mutex = new Object();
    private Handler mHandler;
    private int mType = 0;
    
    private FileOutputStream mCache = null;
    
    private final static String TAG = "PcmRecorder";
    
    public PcmRecorder(Handler handler, int type){
        mHandler = handler;
        mType = type;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void run() {
        Log.v(TAG, "PcmRecorder Start To recording:");
        
        android.os.Process
            .setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        
        Encoder encoder = null;
        try {
            encoder = new Encoder(mHandler, mType);
        } catch (IOException e) {
            Log.v(TAG, "run exception:"+e.toString());
            return;
        }
        
        // 通知录音进程已经开始
        Message message = mHandler.obtainMessage();
        message.what = Params.RECORD_START;
        mHandler.sendMessage(message);
        Log.v(TAG, "sendMessage:"+mHandler);
        
        int bufferRead = 0;
        int bufferSize = AudioRecord.getMinBufferSize(Params.mFrequency,
                AudioFormat.CHANNEL_CONFIGURATION_MONO, Params.mFormat);
        
        // 保证读取的字节尽量为需要编码的整数
        int frameSize = Speex.getInstance().getEncodeFrameSize();
        
        // 保证buffer大小不会小于AudioRecord的最小值
        if ((bufferSize%frameSize) > 0) {
            bufferSize += frameSize;
        }
        
        bufferSize = (bufferSize/frameSize)*frameSize;
        
        short[] tempBuffer = new short[bufferSize/2];
        byte[] byteBuffer = new byte[bufferSize];
        AudioRecord recordInstance = new AudioRecord(
                MediaRecorder.AudioSource.MIC, Params.mFrequency,
                AudioFormat.CHANNEL_CONFIGURATION_MONO, Params.mFormat, bufferSize);
        
        Log.v(TAG, "AudioRecord state is "+recordInstance.getState());
        if (recordInstance.getState() == AudioRecord.STATE_INITIALIZED){
            Thread encodeThread = new Thread(encoder);
            encoder.start();
            encodeThread.start();
            
            recordInstance.startRecording();
            
            while (isRunning()) {
    
                bufferRead = recordInstance.read(tempBuffer, 0, bufferSize/2);
    
                if (bufferRead > 0) {
    //                Log.v(TAG, "read buffer size:"+bufferRead);
                    encoder.putData(tempBuffer, bufferRead);
                    
                    // 写入PCM文件，debug使用
                    try {
                        if (mCache != null){
                            CacheFile.short2byte(tempBuffer, byteBuffer, bufferRead);
                            mCache.write(byteBuffer, 0, bufferRead*2);
                        }
                    } catch (IOException e) {
                        Log.v(TAG, "Write cache exception:"+e.toString());
                    }
                }
            }
            
            try {
                if (mCache != null){
                    mCache.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            recordInstance.stop();
            recordInstance.release();
            
            encoder.stop();
        } else {
            message = mHandler.obtainMessage();
            message.what = Params.RECORD_FINISH;
            message.obj  = null;
            mHandler.sendMessage(message);
            Log.v(TAG, "init audio recorder failed!");
        }
        
        Log.v(TAG, "Stop Recording!");
    }
    
    public void start() {
        setRunning(true);
    }

    public void stop(){
        setRunning(false);
    }
    
    private void setRunning(boolean isRunning) {
        synchronized (mutex) {
            this.isRunning = isRunning;
            mutex.notify();
        }
    }
    
    public boolean isRunning() {
        synchronized (mutex) {
            return isRunning;
        }
    }
}
