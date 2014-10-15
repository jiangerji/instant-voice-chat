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

import com.crazy.x.audio.XDecoder.XDecoderListener;
import com.crazy.x.network.SocketByteBuffer;
import com.crazy.x.utils.CommonUtils;
import com.linekong.voice.util.Params;

public class XPlayer extends Thread {
    private final static String TAG = "XPlayer";

    /*********** debug parameters begin *********/
    public boolean DEBUG_MODE = false;
    private String DEBUG_FILENAME = "/sdcard/s.spx";
    /*********** debug parameters end *********/

    private final Object mMutex = new Object();
    private volatile boolean isRunning = true;

    XDecoder mXDecoder = null;

    private Queue<SocketByteBuffer> mAudioBuffers = new LinkedList<SocketByteBuffer>();

    public void putData(byte[] audioData, int offset, int count) {
        Log.d(TAG, "putData:" + offset + " " + count);
        synchronized (mMutex) {
            mFillBufferFinished = false;
            mAudioBuffers.offer(new SocketByteBuffer(audioData, offset, count));

            mMutex.notify();
        }
    }

    private boolean mFillBufferFinished = false;

    public void finish() {
        Log.d(TAG, "Finish streaming speaking!");
        mFillBufferFinished = true;
    }

    @Override
    public void run() {
        super.run();

        if (DEBUG_MODE) {
            playFile();
        } else {
            playStream();
        }
    }

    @SuppressWarnings("deprecation")
    private void playStream() {
        try {
            android.os.Process
                    .setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            mXDecoder = new XDecoder(new XDecoderListener() {
                FileOutputStream fos = new FileOutputStream(
                        "/sdcard/Xplayer.pcm");
                AudioTrack mAudioTrack = null;

                @Override
                public void onDecoderStop() {
                    mAudioTrack.stop();
                    mAudioTrack.release();

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    try {
                        fos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDecoderStart() {
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
                }

                @Override
                public void onDecoderContent(short[] content, int length) {
                    mAudioTrack.write(content, 0, length);

                    byte[] buffer = new byte[length * 2];
                    CommonUtils.short2byte(content, buffer, length);
                    try {
                        fos.write(buffer, 0, length * 2);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            mXDecoder.startDecoder();

            Log.d(TAG, "Start Play Stream!");
            byte[] tempBuffer = null;
            //            FileOutputStream fos = new FileOutputStream(DEBUG_FILENAME);

            while (isRunning()) {

                synchronized (mMutex) {
                    if (mAudioBuffers.size() > 0) {
                        tempBuffer = mAudioBuffers.poll().array();
                        // fos.write(tempBuffer);
                        mXDecoder.putData(tempBuffer, tempBuffer.length);
                    } else if (mFillBufferFinished) {
                        Log.d(TAG, "Play Finished!");
                        break;
                    } else {
                        mMutex.wait();
                    }
                }
            }

            //            fos.close();

        } catch (Exception e) {
            Log.d(TAG, "Exeption:" + e.toString());
        } finally {

        }
    }

    @SuppressWarnings("deprecation")
    private void playFile() {
        FileInputStream mFileInputStream = null;
        AudioTrack atrack = null;

        try {
            mFileInputStream = new FileInputStream(DEBUG_FILENAME);

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

            final AudioTrack mAudioTrack = atrack;

            mXDecoder = new XDecoder(new XDecoderListener() {
                FileOutputStream fos = new FileOutputStream("/sdcard/p.pcm");

                @Override
                public void onDecoderStop() {
                    mAudioTrack.stop();
                    mAudioTrack.release();

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    try {
                        fos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDecoderStart() {
                }

                @Override
                public void onDecoderContent(short[] content, int length) {
                    mAudioTrack.write(content, 0, length);
                    byte[] buffer = new byte[length * 2];
                    CommonUtils.short2byte(content, buffer, length);
                    try {
                        fos.write(buffer, 0, length * 2);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            mXDecoder.startDecoder();

            int count = 0;
            while (isRunning() && (count = mFileInputStream.read(buffer)) > 0) {
                Log.d(TAG, "write " + count + "bytes");
                mXDecoder.putData(buffer, count);
            }
        } catch (IOException e) {
            Log.d(TAG, "Exeption:" + e.toString());
        } finally {
            if (mXDecoder != null) {
                mXDecoder.stopDecoder();
            }

            if (mFileInputStream != null) {
                try {
                    mFileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 启动播放录音
     */
    public void startPlay() {
        Log.d(TAG, "Start streaming speaking!");
        setRunning(true);
        start();
    }

    /**
     * 停止播放录音
     */
    public void stopPlay() {
        setRunning(false);
        mXDecoder.stopDecoder();
    }

    private void setRunning(boolean isRunning) {
        synchronized (mMutex) {
            this.isRunning = isRunning;
            mMutex.notify();
        }
    }

    public boolean isRunning() {
        synchronized (mMutex) {
            return isRunning;
        }
    }

}
