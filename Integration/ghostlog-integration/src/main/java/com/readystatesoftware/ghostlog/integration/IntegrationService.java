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

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class IntegrationService extends Service {

    private static final String TAG = "IntegrationService";

    private static boolean sIsRunning = false;

    private HandlerThread mLogReaderThread = new HandlerThread("log-reader");
    private Handler mLogReaderHandler;
    private Runnable mLogReader = new Runnable() {
        @Override
        public void run() {
            readLogs();
        }
    };

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

    private void startLogReader() {
        mLogReaderThread.start();
        mLogReaderHandler = new Handler(mLogReaderThread.getLooper());
        mLogReaderHandler.post(mLogReader);
    }

    private void stopLogReader() {
        if (mLogReaderHandler != null) {
            mLogReaderHandler.removeCallbacks(mLogReader);
            mLogReaderThread.quit();
            mLogReaderHandler = null;
        }
    }

    // background stuff

    private boolean readLogs() {

        Process process = null;
        BufferedReader reader = null;
        boolean ok = true;

        try {

            // clear buffer first
            clearLogcatBuffer();

            String[] args = {"logcat", "-v", "threadtime"};
            process = Runtime.getRuntime().exec(args);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()), 8192);

            while (sIsRunning) {
                final String line = reader.readLine();
                if (line != null) {
                    // publish result
                    sendBroadcast(getBroadcastIntent(line), Constants.PERMISSION_READ_LOGS);
                }
            }

        } catch (IOException e) {

            e.printStackTrace();
            ok = false;

        } finally {

            if (process != null) {
                process.destroy();
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN
                    && reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return ok;

    }

    private Intent getBroadcastIntent(String line) {
        Intent intent = new Intent(Constants.ACTION_LOG);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.EXTRA_LINE, line);
        intent.putExtras(bundle);
        return intent;
    }

    private void clearLogcatBuffer() {
        try {
            Process process = Runtime.getRuntime().exec(new String[] {"logcat", "-c"});
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
