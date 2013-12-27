package com.readystatesoftware.ghostlog;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

public class LogWindowService extends Service {

    private static final String TAG = "LogWindowService";
    private static final int NOTIFICATION_ID = 1138;

    private static boolean sIsRunning = false;

    private NotificationManager mNotificationManager;
    private TextView mTestView;

    public LogWindowService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        EventBus.getInstance().register(this);
    }

    private void showNotification() {

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setOngoing(true)
                .setContentTitle("GhostLog Active")
                .setContentText("Blah blah blah...")
                .setContentIntent(getNotificationIntent(null))
                .addAction(0, "Pause", getNotificationIntent(LogReceiver.ACTION_PAUSE))
                .addAction (0, "Clear", getNotificationIntent(LogReceiver.ACTION_CLEAR));

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
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );

        final WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        mTestView = new TextView(this);
        mTestView.setText("TEST");

        wm.addView(mTestView, lp);
    }

    private void removeSytemWindow() {
        try {
            if(mTestView != null && mTestView.getParent() != null) {
                final WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
                wm.removeView(mTestView);
            }
        } catch (Exception e) {
            Log.e(TAG, "Remove window failed");
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
        Log.d(TAG, "onPlayLog");
    }

    @Subscribe
    public void onPauseLog(EventBus.PauseLogEvent event) {
        Log.d(TAG, "onPauseLog");
    }

    @Subscribe
    public void onClearLog(EventBus.ClearLogEvent event) {
        Log.d(TAG, "onClearLog");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sIsRunning = true;
        createSystemWindow();
        showNotification();
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sIsRunning = false;
        removeSytemWindow();
        removeNotification();
        EventBus.getInstance().unregister(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static boolean isRunning() {
        return sIsRunning;
    }
}
