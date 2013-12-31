package com.readystatesoftware.ghostlog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import com.readystatesoftware.ghostlog.integration.Constants;

public class LogReceiver extends BroadcastReceiver {

    public static final String ACTION_PLAY = "com.readystatesoftware.ghostlog.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.readystatesoftware.ghostlog.ACTION_PAUSE";
    public static final String ACTION_CLEAR = "com.readystatesoftware.ghostlog.ACTION_CLEAR";
    public static final String ACTION_SHARE = "com.readystatesoftware.ghostlog.ACTION_SHARE";

    public interface Callbacks {
        public void onLogPause();
        public void onLogResume();
        public void onLogClear();
        public void onLogShare();
        public void onIntegrationDataReceived(String line);
    }

    private Callbacks mCallbacks;

    public LogReceiver(Callbacks callbacks) {
        mCallbacks = callbacks;
    }

    public IntentFilter getIntentFilter() {
        IntentFilter f = new IntentFilter();
        f.addAction(ACTION_PLAY);
        f.addAction(ACTION_PAUSE);
        f.addAction(ACTION_CLEAR);
        f.addAction(ACTION_SHARE);
        f.addAction(Constants.ACTION_LOG);
        return f;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_PLAY.equals(action)) {
            mCallbacks.onLogResume();
        } else if (ACTION_PAUSE.equals(action)) {
            mCallbacks.onLogPause();
        } else if (ACTION_CLEAR.equals(action)) {
            mCallbacks.onLogClear();
        } else if (ACTION_SHARE.equals(action)) {
            mCallbacks.onLogShare();
        } else if (Constants.ACTION_LOG.equals(action)) {
            // handle integration broadcast
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                String line = bundle.getString(Constants.EXTRA_LINE);
                mCallbacks.onIntegrationDataReceived(line);
            }
        }
    }
}
