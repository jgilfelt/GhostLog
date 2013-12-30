package com.readystatesoftware.ghostlog.integration;

import android.os.AsyncTask;
import android.os.Build;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class IntegrationLogReaderAsyncTask extends AsyncTask<Void, String, Boolean> {

    private static final int STREAM_BUFFER_SIZE = 8192;

    @Override
    protected Boolean doInBackground(Void... voids) {

        Process process = null;
        BufferedReader reader = null;
        boolean ok = true;

        try {

            String[] args = {"logcat", "-v", "threadtime"};
            process = Runtime.getRuntime().exec(args);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()), STREAM_BUFFER_SIZE);

            while (!isCancelled()) {
                String line = reader.readLine();
                // wait till our reader is blocking before publishing
                // TODO might want to maintain a small buffer of lines here
                if (!reader.ready() && line != null) {
                    // publish result
                    publishProgress(line);
                }
            }

        } catch (IOException e) {

            e.printStackTrace();
            ok = false;

        } finally {

            if (process != null) {
                process.destroy();
            }

            // post-jellybean, we just kill the process, so there's no need
            // to close the bufferedReader.  Anyway, it just hangs.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN
                    && reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return ok;
    }

}
