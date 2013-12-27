package com.readystatesoftware.ghostlog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class LogReceiver extends BroadcastReceiver {

    public static final String ACTION_PLAY = "com.readystatesoftware.ghostlog.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.readystatesoftware.ghostlog.ACTION_PAUSE";
    public static final String ACTION_CLEAR = "com.readystatesoftware.ghostlog.ACTION_CLEAR";
    public static final String ACTION_SHARE = "com.readystatesoftware.ghostlog.ACTION_SHARE";

    public LogReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_PLAY.equals(action)) {
            EventBus.getInstance().post(new EventBus.PlayLogEvent());
        } else if (ACTION_PAUSE.equals(action)) {
            EventBus.getInstance().post(new EventBus.PauseLogEvent());
        } else if (ACTION_CLEAR.equals(action)) {
            EventBus.getInstance().post(new EventBus.ClearLogEvent());
        } else if (ACTION_SHARE.equals(action)) {
            EventBus.getInstance().post(new EventBus.ShareLogEvent());
        }
    }
}
