package com.readystatesoftware.ghostlog;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class ShareActivity extends Activity {
    // this activity exists so we can launch the share chooser
    // from a notification action - see LogService.onShareLog()
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // simply post the share event and finish
        Intent intent = new Intent(LogReceiver.ACTION_SHARE);
        sendBroadcast(intent);
        finish();
    }
}
