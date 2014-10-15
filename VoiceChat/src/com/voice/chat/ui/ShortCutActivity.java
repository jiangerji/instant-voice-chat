package com.voice.chat.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.voice.chat.service.ShortCutService;

public class ShortCutActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        intent.setClass(this, ShortCutService.class);
        startService(intent);
        finish();
    }
}
