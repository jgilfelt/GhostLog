package com.readystatesoftware.ghostlog;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.Log;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

public class LogWindowService extends Service {

    private static boolean sIsRunning = false;

    private TextView mTestView;

    public LogWindowService() {
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
            Log.e("GhostLog", "Remove window failed");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sIsRunning = true;
        createSystemWindow();
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sIsRunning = false;
        removeSytemWindow();
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
