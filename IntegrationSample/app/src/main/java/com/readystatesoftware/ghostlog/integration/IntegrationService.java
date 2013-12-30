package com.readystatesoftware.ghostlog.integration;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

public class IntegrationService extends Service {

    private static boolean sIsRunning = false;

    public static boolean isRunning() {
        return sIsRunning;
    }

    public IntegrationService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sIsRunning = true;
        Toast.makeText(this, "started integration service", Toast.LENGTH_LONG).show();
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sIsRunning = false;
        Toast.makeText(this, "stopped integration service", Toast.LENGTH_LONG).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
