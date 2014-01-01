/*
 * Copyright (C) 2013 readyState Software Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.readystatesoftware.ghostlog.integration;

import android.os.AsyncTask;
import android.os.Build;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class IntegrationLogReaderAsyncTask extends AsyncTask<Void, String, Boolean> {

    @Override
    protected Boolean doInBackground(Void... voids) {

        Process process = null;
        BufferedReader reader = null;
        boolean ok = true;

        try {

            // clear buffer first
            clearLogcatBuffer();

            String[] args = {"logcat", "-v", "threadtime"};
            process = Runtime.getRuntime().exec(args);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()), 8192);

            while (!isCancelled()) {
                final String line = reader.readLine();
                if (line != null) {
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

    private void clearLogcatBuffer() {
        try {
            Process process = Runtime.getRuntime().exec(new String[] {"logcat", "-c"});
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
