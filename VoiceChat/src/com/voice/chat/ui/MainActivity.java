package com.voice.chat.ui;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.voice.chat.R;
import com.voice.chat.receiver.ScheduleAlarmReceiver;
import com.voice.chat.utils.PhoneGameDataBase;
import com.voice.chat.utils.PlatformUtil;

public class MainActivity extends Activity {

    private final static String TAG = "voice";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClick(View view) {

        switch (view.getId()) {
        case R.id.addShort:
            sendGameShortCut(this);
            break;

        case R.id.start:
            ScheduleAlarmReceiver.scheduleAlarms(this);
            Toast.makeText(this, "start alarm service ...... ",
                    Toast.LENGTH_SHORT).show();
            break;

        case R.id.stop:
            ScheduleAlarmReceiver.cancelAlarms(this);
            Toast.makeText(this, "stop alarm service ...... ",
                    Toast.LENGTH_SHORT).show();
            break;

        default:
            break;
        }
    }

    public void sendGameShortCut(Context context) {
        PhoneGameDataBase mPhoneGame = new PhoneGameDataBase(context);
        ArrayList<String> gameList = mPhoneGame.getPhoneGameList();

        for (String game : gameList) {
            PlatformUtil.addShortCut(this, game);
        }

    }
}
