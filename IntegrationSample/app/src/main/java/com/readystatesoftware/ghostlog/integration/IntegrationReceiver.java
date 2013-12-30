package com.readystatesoftware.ghostlog.integration;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

public class IntegrationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, IntegrationService.class);
        String action = intent.getAction();
        if (Constants.ACTION_COMMAND.equals(action)) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                boolean enable = bundle.getBoolean(Constants.EXTRA_ENABLED);
                if (enable) {
                    if (!IntegrationService.isRunning()) {
                        context.startService(serviceIntent);
                    }
                } else {
                    context.stopService(serviceIntent);
                }
            }
        }
    }

}
