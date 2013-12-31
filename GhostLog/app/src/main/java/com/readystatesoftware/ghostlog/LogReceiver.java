/*
 * Copyright (C) 2013 readyState Software Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
