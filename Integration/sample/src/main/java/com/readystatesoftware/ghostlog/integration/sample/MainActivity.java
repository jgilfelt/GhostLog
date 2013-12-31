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

package com.readystatesoftware.ghostlog.integration.sample;

import android.app.Activity;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class MainActivity extends Activity {

    private static final String TAG_HELLO = "Hello";
    private static final String TAG_SPAM = "Spam";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new MainFragment())
                    .commit();
        }
    }

    public static class MainFragment extends Fragment {

        private MySpamTask task;

        public MainFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            Button button = (Button) rootView.findViewById(R.id.btn_hello);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.v(TAG_HELLO, "verbose message");
                    Log.d(TAG_HELLO, "debug message");
                    Log.i(TAG_HELLO, "info message");
                    Log.w(TAG_HELLO, "warning message");
                    Log.e(TAG_HELLO, "error message");
                    Log.wtf(TAG_HELLO, "wtf message");
                }
            });

            button = (Button) rootView.findViewById(R.id.btn_spam);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (task == null) {
                        task = new MySpamTask();
                        task.execute();
                        ((Button) v).setText(R.string.stop_spam);
                    } else {
                        task.cancel(true);
                        task = null;
                        ((Button) v).setText(R.string.start_spam);
                    }
                }
            });

            return rootView;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (task != null) {
                task.cancel(true);
            }
        }
    }

    public static class MySpamTask extends AsyncTask<Void, Integer, Boolean> {
        @Override
        protected Boolean doInBackground(Void... arg0) {
            Log.d(TAG_SPAM, "start spam");
            while (true) {
                if (isCancelled()) {
                    return true;
                }
                int random = (int)(Math.random() * ((Cheeses.sCheeseStrings.length-1) + 1));
                Log.i(TAG_SPAM, Cheeses.sCheeseStrings[random]);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
            }
        }
        @Override
        protected void onCancelled(Boolean result) {
            Log.d(TAG_SPAM, "stop spam");
        }
    }

}
