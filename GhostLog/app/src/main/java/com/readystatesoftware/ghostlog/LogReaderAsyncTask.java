package com.readystatesoftware.ghostlog;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.nolanlawson.logcat.data.LogLine;
import com.nolanlawson.logcat.data.LogLineAdapter;
import com.nolanlawson.logcat.reader.LogcatReader;
import com.nolanlawson.logcat.reader.LogcatReaderLoader;
import com.nolanlawson.logcat.util.ArrayUtil;

import java.util.LinkedList;

public class LogReaderAsyncTask extends AsyncTask<Void,LogLine,Void> {

    // how often to check to see if we've gone over the max size
    private static final int UPDATE_CHECK_INTERVAL = 200;

    private int counter = 0;
    private volatile boolean paused;
    private final Object lock = new Object();
    private boolean firstLineReceived;
    private boolean killed;
    private LogcatReader reader;
    private Runnable onFinished;

    private LogcatReaderLoader mLoader;
    private LogLineAdapter mAdapter;

    public LogReaderAsyncTask(LogcatReaderLoader loader, LogLineAdapter adapter) {
        mLoader = loader;
        mAdapter = adapter;
    }

    @Override
    protected Void doInBackground(Void... params) {
        Log.d("LOG", "doInBackground()");

        try {
            // use "recordingMode" because we want to load all the existing lines at once
            // for a performance boost
            //LogcatReaderLoader loader = LogcatReaderLoader.create(mContext, true);
            reader = mLoader.loadReader();

            int maxLines = 1000; //PreferenceHelper.getDisplayLimitPreference(LogcatActivity.this);

            String line;
            LinkedList<LogLine> initialLines = new LinkedList<LogLine>();
            while ((line = reader.readLine()) != null) {
                if (paused) {
                    synchronized (lock) {
                        if (paused) {
                            lock.wait();
                        }
                    }
                }
                LogLine logLine = LogLine.newLogLine(line, true);
                if (!reader.readyToRecord()) {
                    // "ready to record" in this case means all the initial lines have been flushed from the reader
                    initialLines.add(logLine);
                    if (initialLines.size() > maxLines) {
                        initialLines.removeFirst();
                    }
                } else if (!initialLines.isEmpty()) {
                    // flush all the initial lines we've loaded
                    initialLines.add(logLine);
                    publishProgress(ArrayUtil.toArray(initialLines, LogLine.class));
                    initialLines.clear();
                } else {
                    // just proceed as normal
                    publishProgress(logLine);
                }
            }
        } catch (InterruptedException e) {
            Log.e("LOG", "expected error", e);
        } catch (Exception e) {
            Log.e("LOG", "unexpected error", e);
        } finally {
            killReader();
            Log.d("LOG", "AsyncTask has died");
        }

        return null;
    }

    public void killReader() {
        if (!killed) {
            synchronized (lock) {
                if (!killed && reader != null) {
                    reader.killQuietly();
                    killed = true;
                }
            }
        }

    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        Log.d("LOG","onPostExecute()");
        doWhenFinished();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        Log.d("LOG","onPreExecute()");

        //resetDisplayedLog(null);

        //showProgressBar();
    }

    @Override
    protected void onProgressUpdate(LogLine... values) {
        super.onProgressUpdate(values);

        if (!firstLineReceived) {
            firstLineReceived = true;
            //hideProgressBar();
        }
        for (LogLine logLine : values) {
            mAdapter.add(logLine);
            //addToAutocompleteSuggestions(logLine);
        }

        // how many logs to keep in memory?  this avoids OutOfMemoryErrors
        int maxNumLogLines = 1000; //PreferenceHelper.getDisplayLimitPreference(LogcatActivity.this);

        // check to see if the list needs to be truncated to avoid out of memory errors
        if (++counter % UPDATE_CHECK_INTERVAL == 0
                && mAdapter.getTrueValues().size() > maxNumLogLines) {
            int numItemsToRemove = mAdapter.getTrueValues().size() - maxNumLogLines;
            mAdapter.removeFirst(numItemsToRemove);
            Log.d("LOG","truncating " + numItemsToRemove + " lines from log list to avoid out of memory errors");
        }

        //if (autoscrollToBottom) {
        //    getListView().setSelection(getListView().getCount());
        //}

    }

    private void doWhenFinished() {
        if (paused) {
            unpause();
        }
        if (onFinished != null) {
            onFinished.run();
        }
    }

    public void pause() {
        synchronized (lock) {
            paused = true;
        }
    }

    public void unpause() {
        synchronized (lock) {
            paused = false;
            lock.notify();
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public void setOnFinished(Runnable onFinished) {
        this.onFinished = onFinished;
    }


}
