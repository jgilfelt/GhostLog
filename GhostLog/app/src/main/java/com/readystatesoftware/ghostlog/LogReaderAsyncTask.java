package com.readystatesoftware.ghostlog;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class LogReaderAsyncTask extends AsyncTask<Void, LogLine, Void> {

    private static final String CMD_LOGCAT = "logcat -v threadtime *:V\n";
    private static final String CMD_SU = "su";
    private static final int STREAM_BUFFER_SIZE = 4096;

    @Override
    protected Void doInBackground(Void... voids) {

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
                if(line.length() == 0) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {

                    }
                    continue;
                }
                publishProgress(new LogLine(line));
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

}
