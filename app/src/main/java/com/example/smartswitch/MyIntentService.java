package com.example.smartswitch;

import android.app.IntentService;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MyIntentService extends IntentService {

    public static final String MY_INTENT_SERVICE = "MyIntentService";

    public MyIntentService() {
        super("MyIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String DATA = intent.getStringExtra("DATA");

        Intent resultintent = new Intent(MY_INTENT_SERVICE);
        resultintent.putExtra("DATA", DATA);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(resultintent);
    }
}
