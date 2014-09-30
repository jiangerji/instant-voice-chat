package com.linekong.voice.io;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Message;

import com.linekong.voice.util.ByteBuffer;
import com.linekong.voice.util.CacheFile;
import com.linekong.voice.util.Params;

public class FilePlayerConsumer extends Consumer {
    private static final String TAG = "FilePlayerConsumer";
    
    private FileOutputStream mCache = null;
    private String mFilename;
    private String mAudioID;
    private Handler mHandler;
    
    public FilePlayerConsumer(Handler handler, String audioID, int type) throws FileNotFoundException{
        mFilename = audioID+".pcm";
        mAudioID = audioID;
        mCache = CacheFile.openCacheWriteStream(mFilename, type);
        mHandler = handler;
    }
    
    boolean mIsPlaying = false;
    public void stopPlay(){
        mIsPlaying = false;
    }

    @Override
    public int doProcess(ByteBuffer byteBuffer) {
        try {
            if (mCache != null){
                mCache.write(byteBuffer.mData, 0, byteBuffer.mSize);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void doFinish() {
        try {
            mCache.close();
            
            playSound();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @SuppressWarnings("deprecation")
    public void playSound(){
        FileInputStream mFileInputStream  = null;
        AudioTrack atrack = null;
        mIsPlaying = true;
        try {
            mFileInputStream = CacheFile.openCacheReadStream(mFilename);
            
            byte[] buffer = new byte[1024];
            android.os.Process
                .setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
    
            int bufSize = AudioTrack.getMinBufferSize(
                    Params.mFrequency,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    Params.mFormat);
            atrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    Params.mFrequency,
                    AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    Params.mFormat, bufSize, AudioTrack.MODE_STREAM);
            
            atrack.setPlaybackRate(Params.mFrequency);
            atrack.play();
            
            mHandler.sendEmptyMessage(Params.PLAY_START);
            
            int count = 0;
            while (mIsPlaying && (count = mFileInputStream.read(buffer)) > 0) {
                atrack.write(buffer, 0, count);
            }
        } catch (IOException e){
            
        } finally {
            if (mFileInputStream != null) {
                try {
                    mFileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            atrack.stop();
            atrack.release();
            
            // 删除cache文件
            CacheFile.deleteCacheFile(mFilename);
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // 发送播放完毕消息
            mIsPlaying = false;
            
            Message msg = mHandler.obtainMessage(Params.PLAY_FINISH);
            msg.obj = mAudioID;
            mHandler.sendMessage(msg);
        }
    }

    /**
     * 打log用的
     */
    @Override
    public String getTag() {
        return TAG;
    }

}
