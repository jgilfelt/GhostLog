package com.readystatesoftware.ghostlog;

import android.app.ActivityManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.util.Pair;

import com.nolanlawson.logcat.helper.LogcatHelper;
import com.nolanlawson.logcat.helper.RuntimeHelper;
import com.nolanlawson.logcat.helper.SuperUserHelper;
import com.nolanlawson.logcat.reader.LogcatReader;
import com.nolanlawson.logcat.reader.LogcatReaderLoader;
import com.nolanlawson.logcat.util.ArrayUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

public class LogReaderAsyncTask extends AsyncTask<ActivityManager, LogReaderAsyncTask.LogUpdate, Boolean> {

    private static final String TAG = "LogReaderAsyncTask";
    private static final int STREAM_BUFFER_SIZE = 8192;


    private int mPidCache = 0;

    private final Object mLock = new Object();
    private boolean mFirstLineReceived;
    private boolean mKilled;
    private LogcatReader mReader;
    private LogcatReaderLoader mLoader;

    public LogReaderAsyncTask(Context context) {
        mLoader = LogcatReaderLoader.create(context, true);
    }

    @Override
    protected Boolean doInBackground(ActivityManager... ams) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && !SuperUserHelper.requestRoot()) {
            return false;
        }

        //Process process = null;
        //BufferedReader reader = null;
        boolean ok = true;



        try {

//            process = LogcatHelper.getLogcatProcess(LogcatHelper.BUFFER_MAIN);
//            reader = new BufferedReader(new InputStreamReader(process.getInputStream()), STREAM_BUFFER_SIZE);
//
//            boolean consumedInitialBuffer = false;
//
//            while (!isCancelled()) {
//                String line = reader.readLine();
//                // wait till our reader is blocking before publishing
//                // TODO might want to maintain a small buffer of lines here
//                //if (!reader.ready() && line != null) {
//                if (consumedInitialBuffer || !reader.ready()) {
//                    consumedInitialBuffer = true;
//                    // publish result
//                    if (line != null) {
//                        final Pair<Integer, String> p = getForegroundApp(ams[0]);
//                        final LogUpdate update = new LogUpdate(new LogLine(line), p.first, p.second);
//                        publishProgress(update);
//                    }
//                }
//            }


            mReader = mLoader.loadReader();
            int maxLines = 2000;

            String line;
            LinkedList<LogUpdate> initialLines = new LinkedList<LogUpdate>();
            while ((line = mReader.readLine()) != null) {

                if (isCancelled()) {
                    break;
                }



                if (!mReader.readyToRecord()) {
                    // "ready to record" in this case means all the initial lines have been flushed from the reader
                    //initialLines.add(update);
                    //if (initialLines.size() > maxLines) {
                    //    initialLines.removeFirst();
                    //}
                } else if (!initialLines.isEmpty()) {
                    // flush all the initial lines we've loaded
                    //initialLines.add(update);
                    ////publishProgress(ArrayUtil.toArray(initialLines, LogLine.class));
                    //publishProgress(update);
                    //initialLines.clear();
                } else {

                    final Pair<Integer, String> p = getForegroundApp(ams[0]);
                    final LogLine logLine = new LogLine(line); //LogLine.newLogLine(line, !collapsedMode);
                    final LogUpdate update = new LogUpdate(logLine, p.first, p.second);

                    // just proceed as normal
                    publishProgress(update);
                }
            }



        } catch (IOException e) {

            e.printStackTrace();
            ok = false;

        } finally {

            killReader();

//            if (process != null) {
//                RuntimeHelper.destroy(process);
//            }
//
//            // post-jellybean, we just kill the process, so there's no need
//            // to close the bufferedReader.  Anyway, it just hangs.
//            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN
//                    && reader != null) {
//                try {
//                    reader.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }

        }

        return ok;
    }

    private void killReader() {
        if (!mKilled) {
            synchronized (mLock) {
                if (!mKilled && mReader != null) {
                    mReader.killQuietly();
                    mKilled = true;
                }
            }
        }
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
