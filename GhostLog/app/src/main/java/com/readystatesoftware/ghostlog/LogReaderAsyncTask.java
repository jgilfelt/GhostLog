package com.readystatesoftware.ghostlog;

import android.app.ActivityManager;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;

public class LogReaderAsyncTask extends AsyncTask<ActivityManager, LogReaderAsyncTask.LogUpdate, Void> {

    private static final String TAG = "LogReaderAsyncTask";

    private static final String CMD_LOGCAT = "logcat -v threadtime *:V\n";
    private static final String CMD_SU = "su";
    private static final int STREAM_BUFFER_SIZE = 4096;

    private int mPidCache = 0;

    @Override
    protected Void doInBackground(ActivityManager... ams) {

        Process process = null;
        BufferedReader reader = null;
        BufferedWriter writer = null;

        try {

            process = Runtime.getRuntime().exec(CMD_SU);
            writer = new BufferedWriter( new OutputStreamWriter(process.getOutputStream()), STREAM_BUFFER_SIZE);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()), STREAM_BUFFER_SIZE);
            writer.write(CMD_LOGCAT);
            writer.flush();

            while (!isCancelled()) {
                String line = reader.readLine();
                if (line.length() == 0) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {

                    }
                    continue;
                }

                // TODO wait till our reader is blocking before publishing - might want to maintain
                // a small buffer of lines here for startup
                if (!reader.ready()) {
                    // publish result
                    final Pair<Integer, String> p = getForegroundApp(ams[0]);
                    final LogUpdate update = new LogUpdate(new LogLine(line), p.first, p.second);
                    publishProgress(update);
                }
            }

        } catch (IOException e) {

            e.printStackTrace();

        } finally {

            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (process != null) {
                process.destroy();
            }

        }

        return null;
    }

    private Pair<Integer, String> getForegroundApp(ActivityManager am) {

        final ActivityManager.RunningTaskInfo foregroundTaskInfo = am.getRunningTasks(1).get(0);
        final String pkg = foregroundTaskInfo.topActivity.getPackageName();

        final List<ActivityManager.RunningAppProcessInfo> appProcesses = am.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses){
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                for (String ap : appProcess.pkgList) {
                    if (ap.equals(pkg)) {
                        if (appProcess.pid != mPidCache) {
                            mPidCache = appProcess.pid;
                            Log.i(TAG, "new foreground pid = " + mPidCache);
                        }
                        return new Pair<Integer, String>(appProcess.pid, pkg);
                    }
                }
            }
        }
        return new Pair<Integer, String>(0, pkg);

    }

    public static class LogUpdate {
        public LogLine line;
        public int foregroundPid;
        public String foregroundPkg;

        public LogUpdate(LogLine line, int foregroundPid, String foregroundPkg) {
            this.line = line;
            this.foregroundPid = foregroundPid;
            this.foregroundPkg = foregroundPkg;
        }
    }

}
