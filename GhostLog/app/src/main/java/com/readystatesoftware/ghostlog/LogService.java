package com.readystatesoftware.ghostlog;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.Toast;

import com.readystatesoftware.ghostlog.integration.Constants;
import com.squareup.otto.Subscribe;

import java.util.LinkedList;

public class LogService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "LogService";

    private static final int NOTIFICATION_ID = 1138;
    private static final int LOG_BUFFER_LIMIT = 2000;

    private static boolean sIsRunning = false;

    private boolean mIntegrationEnabled = false;
    private boolean mIsLogPaused = false;
    private String mLogLevel;
    private boolean mAutoFilter;
    private int mForegroundAppPid;
    private String mForegroundAppPkg;
    private String mTagFilter;
    private NotificationManager mNotificationManager;
    private ActivityManager mActivityManager;
    private SharedPreferences mPrefs;
    private ListView mLogListView;
    private LogAdapter mAdapter;
    private LinkedList<LogLine> mLogBuffer;
    private LinkedList<LogLine> mLogBufferFiltered;
    private Handler mLogBufferUpdateHandler = new Handler();
    private LogReaderAsyncTask mLogReaderTask;

    public static boolean isRunning() {
        return sIsRunning;
    }

    public LogService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mActivityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        mLogLevel = mPrefs.getString(getString(R.string.pref_log_level), LogLine.LEVEL_VERBOSE);
        mAutoFilter = mPrefs.getBoolean(getString(R.string.pref_auto_filter), false);
        mTagFilter = mPrefs.getString(getString(R.string.pref_tag_filter), null);
        EventBus.getInstance().register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sIsRunning = true;
        createSystemWindow();
        showNotification();
        startLogReader();
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sIsRunning = false;
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        stopLogReader();
        if (mIntegrationEnabled) {
            sendIntegrationBroadcast(false);
        }
        removeSystemWindow();
        removeNotification();
        EventBus.getInstance().unregister(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not implemented");
    }

    private void showNotification() {

        String level = LogLine.getLevelName(this, mLogLevel);
        String tag = (mTagFilter == null) ? getString(R.string.none) : mTagFilter;

        String smallText = level;
        String bigText = getString(R.string.log_level) + ": " + level + "\n";
        if (mAutoFilter && mForegroundAppPkg != null) {
            smallText = mLogLevel + "/" + mForegroundAppPkg;
            bigText += getString(R.string.auto_filter) + ": " + mForegroundAppPkg + "\n";
        } else {
            bigText += getString(R.string.auto_filter) + ": OFF\n";
        }
        if (!tag.equals(getString(R.string.none))) {
            smallText += "/" + tag;
        }
        bigText += getString(R.string.tag_filter) + ": " + tag;

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
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

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
        try {
            if(mLogListView != null && mLogListView.getParent() != null) {
                final WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
                wm.removeView(mLogListView);
            }
        } catch (Exception e) {
            Log.e(TAG, "Remove window failed");
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
        mLogReaderTask = new LogReaderAsyncTask(this) {
            @Override
            protected void onProgressUpdate(LogUpdate... values) {
                // process the latest logcat line
                boolean change = false;
                if (values[0].foregroundPid != mForegroundAppPid) {
                    change = true;
                }
                mForegroundAppPkg = values[0].foregroundPkg;
                mForegroundAppPid = values[0].foregroundPid;
                updateBuffer(values[0].line);
                if (change) {
                    showNotification();
                }
            }
            @Override
            protected void onPostExecute(Boolean ok) {
                if (!ok) {
                    Toast.makeText(LogService.this, R.string.toast_no_root, Toast.LENGTH_LONG).show();
                    // no root, enable integration
                    mIntegrationEnabled = true;
                    sendIntegrationBroadcast(true);
                }
            }
        };
        mLogReaderTask.execute(mActivityManager);
        Log.d(TAG, "log reader task started");
    }

    private void stopLogReader() {
        if (mLogReaderTask != null) {
            mLogReaderTask.cancel(true);
        }
        mLogReaderTask = null;
        Log.d(TAG, "log reader task stopped");
    }

    private void updateBuffer() {
        updateBuffer(null);
    }

    private void updateBuffer(final LogLine line) {
        mLogBufferUpdateHandler.post( new Runnable() {
            @Override
            public void run() {

                // update raw buffer
                if (line != null) {
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
            if (mTagFilter != null && !mTagFilter.equals(getString(R.string.none))) {
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
            Intent intent = new Intent(getApplicationContext(), LogReceiver.class);
            intent.setAction(action);
            return PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }
    }

    @Subscribe
    public void onPlayLog(EventBus.PlayLogEvent event) {
        mIsLogPaused = false;
        updateBuffer();
        showNotification();
    }

    @Subscribe
    public void onPauseLog(EventBus.PauseLogEvent event) {
        mIsLogPaused = true;
        showNotification();
    }

    @Subscribe
    public void onClearLog(EventBus.ClearLogEvent event) {
        mLogBuffer = new LinkedList<LogLine>();
        updateBuffer();
    }

    @Subscribe
    public void onShareLog(EventBus.ShareLogEvent event) {
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

    @Subscribe
    public void onIntegrationDataReceived(EventBus.IntegrationDataReceivedEvent event) {
        if (mIntegrationEnabled) {
            updateBuffer(new LogLine(event.line));
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
            showNotification();
            updateBuffer();
        } else if (key.equals(getString(R.string.pref_tag_filter))) {
            mTagFilter = mPrefs.getString(getString(R.string.pref_tag_filter), null);
            showNotification();
            updateBuffer();
        }
    }
}
