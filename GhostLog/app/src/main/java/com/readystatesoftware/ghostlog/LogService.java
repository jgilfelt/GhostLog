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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListView;

import com.readystatesoftware.ghostlog.integration.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

public class LogService extends Service implements
        SharedPreferences.OnSharedPreferenceChangeListener, LogReceiver.Callbacks {

    public static final String ACTION_ROOT_FAILED = "com.readystatesoftware.ghostlog.ROOT_FAILED";

    private static final String TAG = "LogService";
    private static final int NOTIFICATION_ID = 1138;
    private static final int LOG_BUFFER_LIMIT = 2000;
    private static final SimpleDateFormat LOGCAT_TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    private static boolean sIsRunning = false;

    private boolean mIntegrationEnabled = false;
    private boolean mIsLogPaused = false;
    private String mLogLevel;
    private boolean mAutoFilter;
    private int mForegroundAppPid;
    private String mForegroundAppPkg;
    private String mTagFilter;
    private NotificationManager mNotificationManager;
    private SharedPreferences mPrefs;
    private ListView mLogListView;
    private LogAdapter mAdapter;
    private LinkedList<LogLine> mLogBuffer;
    private LinkedList<LogLine> mLogBufferFiltered;
    private LogReceiver mLogReceiver;

    private Handler mLogBufferUpdateHandler = new Handler();
    private LogReaderAsyncTask mLogReaderTask;
    private ProcessMonitorAsyncTask mProcessMonitorTask;

    public static boolean isRunning() {
        return sIsRunning;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        mLogLevel = mPrefs.getString(getString(R.string.pref_log_level), LogLine.LEVEL_VERBOSE);
        mAutoFilter = mPrefs.getBoolean(getString(R.string.pref_auto_filter), false);
        mTagFilter = mPrefs.getString(getString(R.string.pref_tag_filter), null);
        mLogReceiver = new LogReceiver(this);
        registerReceiver(mLogReceiver, mLogReceiver.getIntentFilter());

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sIsRunning = true;
        createSystemWindow();
        showNotification();
        startLogReader();
        if (mAutoFilter) {
            startProcessMonitor();
        }
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sIsRunning = false;
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        unregisterReceiver(mLogReceiver);
        stopLogReader();
        if (mIntegrationEnabled) {
            sendIntegrationBroadcast(false);
        }
        stopProcessMonitor();
        removeSystemWindow();
        removeNotification();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not implemented");
    }

    private void showNotification() {

        String level = LogLine.getLevelName(this, mLogLevel);

        String smallText = level;
        String bigText = getString(R.string.log_level) + ": " + level;
        if (mAutoFilter && mForegroundAppPkg != null) {
            smallText = mLogLevel + "/" + mForegroundAppPkg;
            bigText += "\n" + getString(R.string.auto_filter) + ": " + mForegroundAppPkg;
        } else {
            bigText += "\n" + getString(R.string.auto_filter) + ": " + getString(R.string.off);
        }
        if (mTagFilter != null) {
            smallText += "/" + mTagFilter;
            bigText += "\n" + getString(R.string.tag_filter) + ": " + mTagFilter;
        } else {
            bigText += "\n" + getString(R.string.tag_filter) + ": " + getString(R.string.none);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(bigText))
                .setSmallIcon(R.drawable.ic_stat_ghost)
                .setOngoing(true)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(smallText)
                .setContentIntent(getNotificationIntent(null));
        if (mIsLogPaused) {
            mBuilder.addAction(R.drawable.ic_action_play, getString(R.string.play),
                    getNotificationIntent(LogReceiver.ACTION_PLAY));
        } else {
            mBuilder.addAction(R.drawable.ic_action_pause, getString(R.string.pause),
                    getNotificationIntent(LogReceiver.ACTION_PAUSE));
        }
        mBuilder.addAction(R.drawable.ic_action_clear, getString(R.string.clear),
                getNotificationIntent(LogReceiver.ACTION_CLEAR))
                .addAction (R.drawable.ic_action_share, getString(R.string.share),
                        getNotificationIntent(LogReceiver.ACTION_SHARE));

        // issue the notification
        startForeground(NOTIFICATION_ID, mBuilder.build());

    }

    private void removeNotification() {
        // cancel the notification
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    private void createSystemWindow() {
        final WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                //WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                0,
                PixelFormat.TRANSLUCENT
        );

        final LayoutInflater inflator = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        mLogListView = (ListView) inflator.inflate(R.layout.window_log, null);
        setSystemViewBackground();
        mLogBuffer = new LinkedList<LogLine>();
        mLogBufferFiltered = new LinkedList<LogLine>();
        mAdapter = new LogAdapter(this, mLogBufferFiltered);
        mLogListView.setAdapter(mAdapter);
        wm.addView(mLogListView, lp);
    }

    private void removeSystemWindow() {
        if (mLogListView != null && mLogListView.getParent() != null) {
            final WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            wm.removeView(mLogListView);
        }
    }

    private void sendIntegrationBroadcast(boolean enable) {
        Intent intent = new Intent(Constants.ACTION_COMMAND);
        Bundle bundle = new Bundle();
        bundle.putBoolean(Constants.EXTRA_ENABLED, enable);
        intent.putExtras(bundle);
        sendBroadcast(intent);
    }

    private void startLogReader() {
        mLogBuffer = new LinkedList<LogLine>();
        mLogBufferFiltered = new LinkedList<LogLine>();
        mLogReaderTask = new LogReaderAsyncTask() {
            @Override
            protected void onProgressUpdate(LogLine... values) {
                // process the latest logcat lines
                for (LogLine line : values) {
                    updateBuffer(line);
                }
            }
            @Override
            protected void onPostExecute(Boolean ok) {
                if (!ok) {
                    // not root - notify activity
                    LocalBroadcastManager.getInstance(LogService.this)
                            .sendBroadcast(new Intent(ACTION_ROOT_FAILED));
                    // enable integration
                    mIntegrationEnabled = true;
                    sendIntegrationBroadcast(true);
                    updateBuffer(new LogLine("0 " + LOGCAT_TIME_FORMAT.format(new Date())
                            + " 0 0 " + getString(R.string.canned_integration_log_line)));
                }
            }
        };
        mLogReaderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        Log.i(TAG, "Log reader task started");
    }

    private void stopLogReader() {
        if (mLogReaderTask != null) {
            mLogReaderTask.cancel(true);
        }
        mLogReaderTask = null;
        Log.i(TAG, "Log reader task stopped");
    }

    private void startProcessMonitor() {
        mProcessMonitorTask = new ProcessMonitorAsyncTask(this) {
            @Override
            protected void onProgressUpdate(ForegroundProcessInfo... values) {
                boolean change = false;
                if (values[0].pid != mForegroundAppPid) {
                    change = true;
                }
                mForegroundAppPkg = values[0].pkg;
                mForegroundAppPid = values[0].pid;
                updateBuffer();
                if (change) {
                    showNotification();
                }
            }
        };
        mProcessMonitorTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        Log.i(TAG, "process monitor task started");
    }

    private void stopProcessMonitor() {
        if (mProcessMonitorTask != null) {
            mProcessMonitorTask.cancel(true);
        }
        mProcessMonitorTask = null;
        Log.i(TAG, "process monitor task stopped");
    }

    private void updateBuffer() {
        updateBuffer(null);
    }

    private void updateBuffer(final LogLine line) {
        mLogBufferUpdateHandler.post( new Runnable() {
            @Override
            public void run() {

                // update raw buffer
                if (line != null && line.getLevel() != null) {
                    mLogBuffer.add(line);
                }

                // update filtered buffer
                mLogBufferFiltered.clear();
                for (LogLine bufferedLine : mLogBuffer) {
                    if (!isFiltered(bufferedLine)) {
                        mLogBufferFiltered.add(bufferedLine);
                    }
                }

                // update adapter
                if (!mIsLogPaused) {
                    mAdapter.setData(mLogBufferFiltered);
                }

                // purge old entries
                while(mLogBuffer.size() > LOG_BUFFER_LIMIT) {
                    mLogBuffer.remove();
                }

            }
        });
    }

    private boolean isFiltered(LogLine line) {
        if (line != null) {
            if (mAutoFilter && mForegroundAppPid != 0) {
                if (line.getPid() != mForegroundAppPid) {
                    return true;
                }
            }
            if (!LogLine.LEVEL_VERBOSE.equals(mLogLevel)) {
                if (line.getLevel() != null && !line.getLevel().equals(mLogLevel)) {
                    return true;
                }
            }
            if (mTagFilter != null) {
                if (line.getTag() == null || !line.getTag().toLowerCase().contains(mTagFilter.toLowerCase())) {
                    return true;
                }
            }
            return false;
        } else {
            return true;
        }
    }

    private void setSystemViewBackground() {
        int v = mPrefs.getInt(getString(R.string.pref_bg_opacity), 0);
        int level = 0;
        if (v > 0) {
            int a = (int) ((float)v/100f * 255);
            mLogListView.setBackgroundColor(Color.argb(a, level, level, level));
        } else {
            mLogListView.setBackgroundDrawable(null);
        }
    }

    private PendingIntent getNotificationIntent(String action) {
        if (action == null) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            return PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        } else if (action == LogReceiver.ACTION_SHARE) {
            Intent intent = new Intent(getApplicationContext(), ShareActivity.class);
            return PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        } else {
            Intent intent = new Intent(action);
            return PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (mAdapter != null) {
            mAdapter.updateAppearance();
        }
        if (key.equals(getString(R.string.pref_bg_opacity))) {
            setSystemViewBackground();
        } else if (key.equals(getString(R.string.pref_log_level))) {
            mLogLevel = mPrefs.getString(getString(R.string.pref_log_level), LogLine.LEVEL_VERBOSE);
            showNotification();
            updateBuffer();
        } else if (key.equals(getString(R.string.pref_auto_filter))) {
            mAutoFilter = mPrefs.getBoolean(getString(R.string.pref_auto_filter), false);
            if (mAutoFilter) {
                startProcessMonitor();
            } else {
                stopProcessMonitor();
            }
            showNotification();
            updateBuffer();
        } else if (key.equals(getString(R.string.pref_tag_filter))) {
            mTagFilter = mPrefs.getString(getString(R.string.pref_tag_filter), null);
            showNotification();
            updateBuffer();
        }
    }

    @Override
    public void onLogPause() {
        mIsLogPaused = true;
        showNotification();
    }

    @Override
    public void onLogResume() {
        mIsLogPaused = false;
        updateBuffer();
        showNotification();
    }

    @Override
    public void onLogClear() {
        mLogBuffer = new LinkedList<LogLine>();
        updateBuffer();
    }

    @Override
    public void onLogShare() {
        StringBuffer sb = new StringBuffer();
        for (LogLine line : mLogBufferFiltered) {
            sb.append(line.getRaw());
            sb.append("\n");
        }
        Time now = new Time();
        now.setToNow();
        String ts = now.format3339(false);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject) + " " + ts);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(shareIntent);
    }

    @Override
    public void onIntegrationDataReceived(String line) {
        if (mIntegrationEnabled) {
            updateBuffer(new LogLine(line));
        }
    }

}
