package com.readystatesoftware.ghostlog;

import android.app.Activity;
import android.os.Bundle;

public class ShareActivity extends Activity {
    // this activity exists so we can launch the share chooser
    // from a notification action - see LogService.onShareLog()
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // simply post the share event and finish
        EventBus.getInstance().post(new EventBus.ShareLogEvent());
        finish();
    }
}
