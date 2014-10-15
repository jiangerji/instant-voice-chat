package com.voice.chat.floating;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.crazy.x.audio.XRecorder;
import com.crazy.x.audio.XRecorder.PCMRecorderListener;
import com.crazy.x.network.SocketCmdUtils;
import com.crazy.x.network.SocketThreadManager;
import com.linekong.voice.core.Speex;
import com.voice.chat.R;

public class FloatWindow extends FloatWindowService {

    FloatView window = null;
    SocketThreadManager socketThreadManager = null;
    XRecorder xRecorder;

    @Override
    public void startChatService() {
        Log.d(TAG, "startChatService");

        socketThreadManager = SocketThreadManager.sharedInstance();
        socketThreadManager.startThreads();

        Speex.init(0);
    }

    @Override
    public void stopChatService() {
        Log.d(TAG, "stopChatService");

        if (xRecorder != null) {
            xRecorder.stopRecord();
        }
        socketThreadManager.stopThreads();
        Speex.deinit();
    }

    @Override
    public String getAppName() {
        return "ÓïÁÄ";
    }

    @Override
    public int getAppIcon() {
        return R.drawable.ic_micro_phone;
    }

    @Override
    public void createAndAttachView(int id, FrameLayout frame) {
        // create a new layout from body.xml
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.simple, frame, true);
        final ImageView imageView = (ImageView) view
                .findViewById(R.id.micro_btn);
        imageView.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                Log.d(TAG, "onLongClick");
                socketThreadManager.sendMsg(SocketCmdUtils.sendSpeakingStart());

                xRecorder = new XRecorder(mListener);
                xRecorder.startRecord();
                imageView.setImageResource(R.drawable.micro_phone_pressed);

                return false;
            }
        });

        imageView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick");
                if (xRecorder != null) {
                    xRecorder.stopRecord();
                    imageView.setImageResource(R.drawable.micro_phone_normal);
                }
            }
        });
    }

    // the window will be centered
    @Override
    public StandOutLayoutParams getParams(int id, FloatView window) {
        return new StandOutLayoutParams(id, 250, 250,
                StandOutLayoutParams.RIGHT, 150);
    }

    // move the window by dragging the view
    @Override
    public int getFlags(int id) {
        return super.getFlags(id) | StandOutFlags.FLAG_BODY_MOVE_ENABLE
                | StandOutFlags.FLAG_WINDOW_FOCUSABLE_DISABLE;
    }

    @Override
    public String getPersistentNotificationMessage(int id) {
        return "µã»÷¹Ø±ÕÓïÁÄÐü¸¡´°";
    }

    @Override
    public Intent getPersistentNotificationIntent(int id) {
        return FloatWindowService.getCloseIntent(this, FloatWindow.class, id);
    }

    private FileOutputStream mFileOutputStream = null;
    PCMRecorderListener mListener = new PCMRecorderListener() {

        @Override
        public void onRecordStart() {
            try {
                mFileOutputStream = new FileOutputStream("/sdcard/sender.pcm");
                Log.d("cmd", "open Speaing ");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onRecordFinish() {
            try {
                Log.d("cmd", "close Speaing ");
                mFileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            socketThreadManager.sendMsg(SocketCmdUtils.sendSpeakingStop());
        }

        @Override
        public void onRecordContent(byte[] content, int length) {
            // try {
            // Log.d("cmd", "write Speaing Content ");
            // mFileOutputStream.write(content, 0, length);
            // } catch (IOException e) {
            // e.printStackTrace();
            // }

            Log.d("cmd", "Send Speaing Content:" + length);
            socketThreadManager.sendMsg(SocketCmdUtils.sendSpeakingContent(
                    content, length));
        }

        @Override
        public void onRecondPCMContent(byte[] content, int length) {
            try {
                Log.d("cmd", "write Speaing Content ");
                mFileOutputStream.write(content, 0, length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
}
