package com.readystatesoftware.ghostlog;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;

import com.nolanlawson.logcat.helper.SuperUserHelper;
import com.nolanlawson.logcat.reader.LogcatReader;
import com.nolanlawson.logcat.reader.LogcatReaderLoader;

import java.io.IOException;

public class LogReaderAsyncTask extends AsyncTask<Void, LogLine, Boolean> {

    private static final String TAG = "LogReaderAsyncTask";

    private final Object mLock = new Object();
    private boolean mKilled;
    private LogcatReader mReader;
    private LogcatReaderLoader mLoader;

    public LogReaderAsyncTask(Context context) {
        mLoader = LogcatReaderLoader.create(context, true);
    }

    @Override
    protected Boolean doInBackground(Void... voids) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN
                && !SuperUserHelper.requestRoot()) {
            return false;
        }

        boolean ok = true;

        try {

            mReader = mLoader.loadReader();
            String line;
            while ((line = mReader.readLine()) != null) {

                if (isCancelled()) {
                    break;
                }

                if (mReader.readyToRecord()) {
                    // "ready to record" in this case means all the initial lines have been flushed from the reader
                    // just proceed as normal
                    final LogLine logLine = new LogLine(line);
                    publishProgress(logLine);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            ok = false;
        } finally {
            if (!mKilled) {
                synchronized (mLock) {
                    if (!mKilled && mReader != null) {
                        mReader.killQuietly();
                        mKilled = true;
                    }
                }
            }
        }

        return ok;
    }

}
