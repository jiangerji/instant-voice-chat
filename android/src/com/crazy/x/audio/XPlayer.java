package com.crazy.x.audio;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.crazy.x.network.SocketByteBuffer;
import com.linekong.voice.core.Speex;
import com.linekong.voice.util.Params;

public class XPlayer extends Thread {
    private final static String TAG = "XPlayer";

    private final Object mutex = new Object();
    private volatile boolean isRunning = true;

    private Speex mSpeex = Speex.getInstance();

    private Queue<SocketByteBuffer> mAudioBuffers = new LinkedList<SocketByteBuffer>();

    public void putData(byte[] audioData, int offset, int count) {
        Log.d(TAG, "putData:" + offset + " " + count);
        synchronized (mAudioBuffers) {
            mFillBufferFinished = false;
            mAudioBuffers.offer(new SocketByteBuffer(audioData, offset, count));
        }
    }

    private boolean mFillBufferFinished = false;

    public void finish() {
        mFillBufferFinished = true;
    }

    @Override
    public void run() {
        super.run();

        playStream();
    }

    @SuppressWarnings("deprecation")
    private void playStream() {
        AudioTrack atrack = null;
        try {
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

            Log.d(TAG, "start play stream!");
            byte[] tempBuffer = null;
            FileOutputStream fos = new FileOutputStream("/sdcard/aaa.pcm");
            while (isRunning()) {

                synchronized (mAudioBuffers) {
                    if (mAudioBuffers.size() > 0) {
                        tempBuffer = mAudioBuffers.poll().array();
                        fos.write(tempBuffer, 0, tempBuffer.length);
                        atrack.write(tempBuffer, 0, tempBuffer.length);
                    } else if (mFillBufferFinished) {
                        Log.d(TAG, "Play Finished!");
                        break;
                    }
                }
            }
            fos.close();
        } catch (Exception e) {
            Log.d(TAG, "Exeption:" + e.toString());
        } finally {
            atrack.stop();
            atrack.release();

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void playFile() {
        FileInputStream mFileInputStream = null;
        AudioTrack atrack = null;
        try {
            mFileInputStream = new FileInputStream("/sdcard/a.pcm");

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

            int count = 0;
            while (isRunning() && (count = mFileInputStream.read(buffer)) > 0) {
                Log.d(TAG, "write " + count + "bytes");
                atrack.write(buffer, 0, count);
            }
        } catch (IOException e) {
            Log.d(TAG, "Exeption:" + e.toString());
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

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 启动播放录音
     */
    public void startPlay() {
        setRunning(true);
        start();
    }

    /**
     * 停止播放录音
     */
    public void stopPlay() {
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
