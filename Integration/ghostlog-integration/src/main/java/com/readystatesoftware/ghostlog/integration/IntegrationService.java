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

package com.readystatesoftware.ghostlog.integration;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public class IntegrationService extends Service {

    private static final String TAG = "IntegrationService";

    private static boolean sIsRunning = false;

    private IntegrationLogReaderAsyncTask mLogReaderTask;

    public static boolean isRunning() {
        return sIsRunning;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sIsRunning = true;
        startLogReader();
        Log.i(TAG, "Started Ghost Log integration service");
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sIsRunning = false;
        stopLogReader();
        Log.i(TAG, "Stopped Ghost Log integration service");
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @TargetApi(11)
    private void startLogReader() {
        mLogReaderTask = new IntegrationLogReaderAsyncTask() {
            @Override
            protected void onProgressUpdate(Intent... values) {
                // process the latest logcat line
                sendBroadcast(values[0], Constants.PERMISSION_READ_LOGS);
            }
        };
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            mLogReaderTask.execute();
        } else {
            mLogReaderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

    }

    private void stopLogReader() {
        if (mLogReaderTask != null) {
            mLogReaderTask.cancel(true);
        }
        mLogReaderTask = null;
    }

}
