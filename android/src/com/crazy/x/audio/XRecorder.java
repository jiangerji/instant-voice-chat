package com.crazy.x.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.crazy.x.audio.XEncoder.XEncoderListener;
import com.linekong.voice.core.Speex;
import com.linekong.voice.util.Params;

public class XRecorder extends Thread {
    private final static String TAG = "PCMRecorder";

    private final Object mutex = new Object();
    private volatile boolean isRunning = true;

    public static interface PCMRecorderListener {
        public void onRecordStart();

        public void onRecordContent(byte[] content, int length);

        public void onRecordFinish();
    }

    private PCMRecorderListener mListener = null;

    public XRecorder(PCMRecorderListener listener) {
        mListener = listener;
    }

    private XEncoderListener mEncoderListener = new XEncoderListener() {

        @Override
        public void onEncoderStop() {
            if (mListener != null) {
                mListener.onRecordFinish();
            }
        }

        @Override
        public void onEncoderStart() {

        }

        @Override
        public void onEncoderContent(byte[] content, int length) {
            Log.d(TAG, "onEncoderContent: " + length + "bytes");
            if (mListener != null) {
                mListener.onRecordContent(content, length);
            }
        }
    };

    @SuppressWarnings("deprecation")
    @Override
    public void run() {
        super.run();

        android.os.Process
                .setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        XEncoder encoder = new XEncoder(mEncoderListener);
        encoder.startEncoder();

        // 通知录音进程已经开始
        int bufferRead = 0;
        int bufferSize = AudioRecord.getMinBufferSize(Params.mFrequency,
                AudioFormat.CHANNEL_CONFIGURATION_MONO, Params.mFormat);

        // 保证读取的字节尽量为需要编码的整数
        int frameSize = Speex.getInstance().getEncodeFrameSize();

        // 保证buffer大小不会小于AudioRecord的最小值
        if ((bufferSize % frameSize) > 0) {
            bufferSize += frameSize;
        }

        bufferSize = (bufferSize / frameSize) * frameSize;

        short[] tempBuffer = new short[bufferSize / 2];
        //        byte[] byteBuffer = new byte[bufferSize];
        AudioRecord recordInstance = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                Params.mFrequency,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                Params.mFormat,
                bufferSize);

        Log.v(TAG, "AudioRecord state is " + recordInstance.getState());
        if (recordInstance.getState() == AudioRecord.STATE_INITIALIZED) {
            recordInstance.startRecording();
            if (mListener != null) {
                mListener.onRecordStart();
            }

            while (isRunning()) {

                bufferRead = recordInstance.read(tempBuffer, 0, bufferSize / 2);
                //                bufferRead = recordInstance.read(byteBuffer, 0, bufferSize);

                if (bufferRead > 0) {
                    // 写入PCM文件，debug使用
                    try {
                        //                        CommonUtils.short2byte(tempBuffer,
                        //                                byteBuffer, bufferRead);

                        encoder.putData(tempBuffer, bufferRead);
                    } catch (Exception e) {
                        Log.v(TAG, "onRecordContent exception:" + e.toString());
                    }
                }
            }

            recordInstance.stop();
            recordInstance.release();
            encoder.stopEncoder();
        } else {
            Log.v(TAG, "init audio recorder failed!");
        }

        Log.v(TAG, "Stop Recording!");
    }

    /**
     * 启动录音
     */
    public void startRecord() {
        setRunning(true);
        start();
    }

    /**
     * 停止录音
     */
    public void stopRecord() {
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
