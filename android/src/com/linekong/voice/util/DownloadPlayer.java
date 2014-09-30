package com.linekong.voice.util;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.linekong.voice.core.Speex;

public class DownloadPlayer extends BufferedOutputStream {
    private final static String TAG = "DownloadPlayer";
    private Speex mSpeex = Speex.getInstance();
    private AudioTrack mAudioTrack;
    private Handler mHandler;
    private String mAudioID;
    
    @SuppressWarnings("deprecation")
    public DownloadPlayer(OutputStream out, Handler handler, String audioID) {
        super(out);
        
        CACHE_SIZE = Speex.mDecodeSize*10;
        mCache = new byte[CACHE_SIZE];
        
        mDecodeInput = new byte[Speex.mDecodeSize];
        mDecodeOuput = new short[Speex.mEncodeSize/2];
        
        android.os.Process
        .setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        int bufSize = AudioTrack.getMinBufferSize(
                Params.mFrequency,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                Params.mFormat);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                Params.mFrequency,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                Params.mFormat, bufSize, AudioTrack.MODE_STREAM);
        
        mAudioTrack.setPlaybackRate(Params.mFrequency);
        mAudioTrack.play();
        
        mHandler = handler;
        mHandler.sendEmptyMessage(Params.PLAY_START);
        
        mAudioID = audioID;
    }

    @Override
    public void write(int oneByte) throws IOException {
        super.write(oneByte);
    }
    
    private int CACHE_SIZE = 200;//200ms的缓存
    private byte[] mCache = null;
    private int mCurrentCacheSize = 0;
    private short[] mDecodeOuput;
    private byte[] mDecodeInput;
    
    private boolean mAudioTrackHasClose = false;
    private boolean mIsPlaying = true;
    
    @Override
    public synchronized void write(byte[] buffer, int offset, int length)
            throws IOException {
        super.write(buffer, offset, length);
        
        if (!mIsPlaying ){
            if (!mAudioTrackHasClose) {
                mAudioTrack.stop();
                mAudioTrack.release();
                mAudioTrackHasClose = true;
            }
            return;
        }
        
        Log.v(TAG, "write: offset="+offset+", length="+length);
        int srcPos = 0;
        int inputSize = Speex.mDecodeSize;
        
        if (mCurrentCacheSize + length > CACHE_SIZE){
            while(mCurrentCacheSize > inputSize){
                System.arraycopy(mCache, srcPos, mDecodeInput, 0, inputSize);
                mCurrentCacheSize -= inputSize;
                srcPos += inputSize;
                
                if (!mIsPlaying){
                    return;
                }
                mSpeex.decode(mDecodeInput, mDecodeOuput, inputSize);
                // TODO: 写入audiotrack
                mAudioTrack.write(mDecodeOuput, 0, mDecodeOuput.length);
            }
            
            if (!mIsPlaying){
                return;
            }
            
            if (mCurrentCacheSize > 0){
                System.arraycopy(mCache, srcPos, mDecodeInput, 0, mCurrentCacheSize);
            }
            
            if (length + mCurrentCacheSize > inputSize){
                srcPos = 0;
                System.arraycopy(buffer, srcPos, mDecodeInput, mCurrentCacheSize, inputSize - mCurrentCacheSize);
                mSpeex.decode(mDecodeInput, mDecodeOuput, inputSize);
                // TODO: 
                mAudioTrack.write(mDecodeOuput, 0, mDecodeOuput.length);
                
                srcPos += (inputSize - mCurrentCacheSize);
                length -= (inputSize - mCurrentCacheSize);
                while (length > inputSize){
                    if (!mIsPlaying){
                        return;
                    }
                    
                    System.arraycopy(buffer, srcPos, mDecodeInput, 0, inputSize);
                    mSpeex.decode(mDecodeInput, mDecodeOuput, inputSize);
                    // TODO:
                    mAudioTrack.write(mDecodeOuput, 0, mDecodeOuput.length);
                    
                    length -= inputSize;
                    srcPos += inputSize;
                }
                
                System.arraycopy(buffer, srcPos, mCache, 0, length);
                mCurrentCacheSize = length;
            } else {
                // 不够一帧数据，缓存起来
                System.arraycopy(mCache, srcPos, mCache, 0, mCurrentCacheSize);
                System.arraycopy(buffer, offset, mCache, mCurrentCacheSize, length);
                
                mCurrentCacheSize += length;
            }
            
        } else {
            System.arraycopy(buffer, offset, mCache, mCurrentCacheSize, length);
            mCurrentCacheSize += length;
        }
    }
    
    
    @Override
    public void write(byte[] buffer) throws IOException {
        super.write(buffer);
        Log.v(TAG, "write to buffer!");
    }
    
    public void stopPlay(){
        Log.v(TAG, "Stream stop play!");
        mIsPlaying = false;
        
        Message msg = mHandler.obtainMessage(Params.PLAY_FINISH);
        msg.obj = mAudioID;
        mHandler.sendMessage(msg);
    }
    
    @Override
    public synchronized void close() throws IOException {
        super.close();
        
        int inputSize = Speex.mDecodeSize;
        int srcPos = 0;
        while (mCurrentCacheSize > inputSize && mIsPlaying){
            System.arraycopy(mCache, srcPos, mDecodeInput, 0, inputSize);
            mSpeex.decode(mDecodeInput, mDecodeOuput, inputSize);
            mAudioTrack.write(mDecodeOuput, 0, mDecodeOuput.length);
            
            srcPos += inputSize;
            mCurrentCacheSize -= inputSize;
        }
        
        if (!mAudioTrackHasClose){
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrackHasClose = true;
        }
        
        Log.v(TAG, "close");
    }
}
