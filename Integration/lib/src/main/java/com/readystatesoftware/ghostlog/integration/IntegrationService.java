package com.readystatesoftware.ghostlog.integration;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class IntegrationService extends Service {

    private static final String TAG = "IntegrationService";

    private static boolean sIsRunning = false;

    private Handler mLogBufferUpdateHandler = new Handler();
    private IntegrationLogReaderAsyncTask mLogReaderTask;

    public static boolean isRunning() {
        return sIsRunning;
    }

    public IntegrationService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sIsRunning = true;
        startLogReader();
        Log.i(TAG, "Started Ghost Log integration service");
        return Service.START_NOT_STICKY;
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
        mLogReaderTask = new IntegrationLogReaderAsyncTask() {
            @Override
            protected void onProgressUpdate(String... values) {
                // process the latest logcat line
                broadcastLine(values[0]);
            }
        };
        mLogReaderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void stopLogReader() {
        if (mLogReaderTask != null) {
            mLogReaderTask.cancel(true);
        }
        mLogReaderTask = null;
    }

    private void broadcastLine(final String line) {
        mLogBufferUpdateHandler.post( new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(Constants.ACTION_LOG);
                Bundle bundle = new Bundle();
                bundle.putString(Constants.EXTRA_LINE, line);
                intent.putExtras(bundle);
                sendBroadcast(intent);
            }
        });
    }

}
