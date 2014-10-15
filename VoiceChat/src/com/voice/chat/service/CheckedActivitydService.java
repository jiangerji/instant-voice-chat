/***
  Copyright (c) 2012 CommonsWare, LLC
  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
  by applicable law or agreed to in writing, software distributed under the
  License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
  OF ANY KIND, either express or implied. See the License for the specific
  language governing permissions and limitations under the License.
  
  From _The Busy Coder's Guide to Android Development_
    http://commonsware.com/Android
 */

package com.voice.chat.service;

import java.util.List;

import android.app.ActivityManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.voice.chat.floating.FloatWindow;
import com.voice.chat.floating.FloatWindowService;
import com.voice.chat.utils.PhoneGameDataBase;
import com.voice.chat.utils.PlatformUtil;

public class CheckedActivitydService extends IntentService {

    private final static String TAG = "voice";

    public CheckedActivitydService() {
        super("CheckedActivitydService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        checkedActivity(getApplicationContext());
    }

    private void checkedActivity(Context context) {
        PhoneGameDataBase mPhoneGame = new PhoneGameDataBase(context);
        ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);

        String currentRunningPackageName = taskInfo.get(0).topActivity
                .getPackageName();

        long value = PlatformUtil.getPreferenceValue("running-time", 0);
        Log.d(TAG, "currentRunningPackageName: " + currentRunningPackageName
                + " " + value + "    | " + System.currentTimeMillis());
        PlatformUtil.setPreferenceValue("running-time", value + 1);
        if (mPhoneGame.isGame(currentRunningPackageName)) {
            Log.d(TAG, "check activity show Float Window");
            FloatWindowService.show(context, FloatWindow.class,
                    FloatWindowService.DEFAULT_ID);
        } else {
            FloatWindowService.closeAll(this, FloatWindow.class);
            stopService(new Intent(this, FloatWindow.class));
        }
    }
}
