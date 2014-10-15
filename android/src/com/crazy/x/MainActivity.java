package com.crazy.x;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;

import com.crazy.x.audio.XPlayer;
import com.crazy.x.audio.XRecorder;
import com.crazy.x.audio.XRecorder.PCMRecorderListener;
import com.crazy.x.network.SocketCmdUtils;
import com.crazy.x.network.SocketThreadManager;
import com.linekong.voice.core.Speex;

public class MainActivity extends Activity {

    private boolean mIsSokcetCStarted = false;
    SocketThreadManager socketThreadManager = null;
    XRecorder xRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        socketThreadManager = SocketThreadManager.sharedInstance();

        View view = findViewById(R.id.socketSwitch);
        view.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!mIsSokcetCStarted) {
                    socketThreadManager.startThreads();
                } else {
                    socketThreadManager.stopThreads();
                }

                mIsSokcetCStarted = !mIsSokcetCStarted;
            }
        });

        view = findViewById(R.id.speakingBtn);
        view.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                socketThreadManager.sendMsg(SocketCmdUtils.sendSpeakingStart());

                xRecorder = new XRecorder(mListener);
                xRecorder.startRecord();

                return false;
            }
        });

        view.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (xRecorder != null) {
                    xRecorder.stopRecord();
                }
            }
        });

        view = findViewById(R.id.playBtn);
        view.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                XPlayer xPlayer = new XPlayer();
                xPlayer.DEBUG_MODE = true;
                xPlayer.startPlay();
            }
        });

        Speex.init(0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Speex.deinit();
    }

    private FileOutputStream mFileOutputStream = null;
    private FileOutputStream mFileOutputStream2 = null;
    PCMRecorderListener mListener = new PCMRecorderListener() {

        @Override
        public void onRecordStart() {
            try {
                mFileOutputStream = new FileOutputStream("/sdcard/s.spx");
                mFileOutputStream2 = new FileOutputStream("/sdcard/s.pcm");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onRecordFinish() {
            try {
                mFileOutputStream.close();
                mFileOutputStream2.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            socketThreadManager.sendMsg(SocketCmdUtils.sendSpeakingStop());
        }

        @Override
        public void onRecordContent(byte[] content, int length) {
            try {
                mFileOutputStream.write(content, 0, length);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Log.d("cmd", "Send Speaing Content:" + length);
            socketThreadManager.sendMsg(SocketCmdUtils.sendSpeakingContent(
                    content, length));
        }

        @Override
        public void onRecondPCMContent(byte[] content, int length) {
            try {
                mFileOutputStream2.write(content, 0, length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
}
