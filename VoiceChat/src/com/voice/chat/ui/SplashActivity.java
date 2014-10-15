package com.voice.chat.ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.voice.chat.R;
import com.voice.chat.utils.PhoneGameDataBase;
import com.voice.chat.utils.PlatformUtil;
import com.voice.chat.utils.WholeGameDataBase;

public class SplashActivity extends Activity {

    private final static String TAG = "voice";

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.splash_activity);

        mContext = this;

        boolean value = PlatformUtil.getPreferenceValue("first_start", true);
        if (value) {
            new LoadData().start();
            PlatformUtil.setPreferenceValue("first_start", false);
        } else {
            Intent intent = new Intent(mContext, MainActivity.class);
            mContext.startActivity(intent);
            finish();
        }
    }

    class LoadData extends Thread {
        public void run() {

            WholeGameDataBase mWholeGame = new WholeGameDataBase(mContext);
            ArrayList<String> packageNameList = new ArrayList<String>();
            try {
                InputStreamReader inputReader = new InputStreamReader(mContext
                        .getResources().getAssets().open("applist.dat"));
                BufferedReader bufReader = new BufferedReader(inputReader);
                String line = "";
                while ((line = bufReader.readLine()) != null)
                    packageNameList.add(line);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mWholeGame.addPackageNameList(packageNameList);

            PhoneGameDataBase mPhoneGame = new PhoneGameDataBase(mContext);
            PackageManager mPackageManager = getPackageManager();
            List<PackageInfo> listPackages = mPackageManager
                    .getInstalledPackages(PackageManager.GET_ACTIVITIES);

            mPackageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES);

            for (PackageInfo info : listPackages) {
                Log.d(TAG, "package: " + info.packageName);
                if (mWholeGame.isGamePackage(info.packageName)) {
                    mPhoneGame.insertPhoneGame(info.packageName);
                }
            }

            Intent intent = new Intent(mContext, MainActivity.class);
            mContext.startActivity(intent);
            finish();
        }
    }
}
