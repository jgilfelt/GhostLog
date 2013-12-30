package com.readystatesoftware.ghostlog;

import android.app.ActivityManager;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.List;

public class ProcessMonitorAsyncTask extends AsyncTask<Void, ProcessMonitorAsyncTask.ForegroundProcessInfo, Void> {

    private static final String TAG = "ProcessMonitorAsyncTask";

    private ActivityManager mActivityManager;
    private int mPidCache = 0;

    public ProcessMonitorAsyncTask(Context context) {
        mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        ForegroundProcessInfo p;
        while (!isCancelled()) {
            p = getForegroundApp(mActivityManager);
            if (p.pid != mPidCache) {
                mPidCache = p.pid;
                Log.i(TAG, "new foreground pid = " + mPidCache);
                publishProgress(p);
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
        }
        return null;
    }

    private ForegroundProcessInfo getForegroundApp(ActivityManager am) {

        final ActivityManager.RunningTaskInfo foregroundTaskInfo = am.getRunningTasks(1).get(0);
        final String pkg = foregroundTaskInfo.topActivity.getPackageName();

        final List<ActivityManager.RunningAppProcessInfo> appProcesses = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses){
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                for (String ap : appProcess.pkgList) {
                    if (ap.equals(pkg)) {
                        return new ForegroundProcessInfo(appProcess.pid, pkg);
                    }
                }
            }
        }
        return new ForegroundProcessInfo(0, pkg);

    }

    public static class ForegroundProcessInfo {
        public int pid;
        public String pkg;
        public ForegroundProcessInfo(int pid, String pkg) {
            this.pid = pid;
            this.pkg = pkg;
        }
    }

}
