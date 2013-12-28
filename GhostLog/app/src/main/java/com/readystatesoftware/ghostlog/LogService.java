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
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListView;

import com.squareup.otto.Subscribe;

import java.util.LinkedList;

public class LogService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "LogService";

    private static final int NOTIFICATION_ID = 1138;
    private static final int LOG_BUFFER_LIMIT = 1000;

    private static boolean sIsRunning = false;

    private boolean mIsLogPaused = false;
    private String mLogLevel;
    private boolean mAutoFilter;
    private int mForegroundAppPid;
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
        removeSystemWindow();
        removeNotification();
        EventBus.getInstance().unregister(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not implemented");
    }

    private void showNotification() {

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setOngoing(true)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText("Blah blah blah...")
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
                //WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN & WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS &
                //WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
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

    private void startLogReader() {
        mLogBuffer = new LinkedList<LogLine>();
        mLogBufferFiltered = new LinkedList<LogLine>();
        mLogReaderTask = new LogReaderAsyncTask() {
            @Override
            protected void onProgressUpdate(Pair<LogLine, Integer>... values) {
                // TODO ???
                mForegroundAppPid = values[0].second;
                updateBuffer(values[0].first);
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

                if (line != null) {
                    mLogBuffer.add(line);
                }

                mLogBufferFiltered.clear();
                for (LogLine bufferedLine : mLogBuffer) {
                    if (!isFiltered(bufferedLine)) {
                        mLogBufferFiltered.add(bufferedLine);
                    }
                }

                if (!mIsLogPaused) {
                    mAdapter.setData(mLogBufferFiltered);
                }
                while(mLogBuffer.size() > LOG_BUFFER_LIMIT) {
                    mLogBuffer.remove();
                }

            }
        });
    }

    private boolean isFiltered(LogLine line) {

        if (line != null) {
            // TODO tag filter
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
            return false;
        } else {
            return true;
        }

    }

    private void setSystemViewBackground() {
        int v = mPrefs.getInt(getString(R.string.pref_bg_opacity), 0);
        if (v > 0) {
            int a = (int) ((float)v/100f * 255);
            mLogListView.setBackgroundColor(Color.argb(a, 0, 0, 0));
        } else {
            mLogListView.setBackgroundColor(0);
        }
    }

    private PendingIntent getNotificationIntent(String action) {
        if (action == null) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
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
        updateBuffer(null);
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
        updateBuffer(null);
    }

    @Subscribe
    public void onShareLog(EventBus.ShareLogEvent event) {
        Log.d(TAG, "onShareLog");
        // TODO
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
            updateBuffer();
        } else if (key.equals(getString(R.string.pref_auto_filter))) {
            mAutoFilter = mPrefs.getBoolean(getString(R.string.pref_auto_filter), false);
            updateBuffer();
        }
    }
}
