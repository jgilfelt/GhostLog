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

package com.readystatesoftware.ghostlog;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.util.List;

public class MainActivity extends BasePreferenceActivity {

    private static final int CODE_TAG_FILTER = 1;

    private static Preference sTagFilterPref;

    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Switch mainSwitch = new Switch(this);
        mainSwitch.setChecked(LogService.isRunning());
        mainSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Intent intent = new Intent(MainActivity.this, LogService.class);
                if (b) {
                    if (!LogService.isRunning()) {
                        startService(intent);
                    }
                } else {
                    stopService(intent);
                }
            }
        });

        final ActionBar bar = getActionBar();
        final ActionBar.LayoutParams lp = new ActionBar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
        lp.rightMargin = getResources().getDimensionPixelSize(R.dimen.main_switch_margin_right);
        bar.setCustomView(mainSwitch, lp);
        bar.setDisplayShowCustomEnabled(true);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!mPrefs.getBoolean(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES, false)) {
            PreferenceManager.setDefaultValues(this, R.xml.pref_filters, true);
            PreferenceManager.setDefaultValues(this, R.xml.pref_appearance, true);
            SharedPreferences.Editor edit = mPrefs.edit();
            edit.putBoolean(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES, true);
            edit.apply();
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupSimplePreferencesScreen();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CODE_TAG_FILTER) {
            if (resultCode == RESULT_OK) {
                mPrefs.edit().putString(getString(R.string.pref_tag_filter), data.getAction()).apply();
                sTagFilterPref.setSummary(data.getAction());
            }
        }
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        if (!isSimplePreferences(this)) {
            loadHeadersFromResource(R.xml.pref_headers, target);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sTagFilterPref = null;
    }

    /**
     * Shows the simplified settings UI if the device configuration if the
     * device configuration dictates that a simplified, single-pane UI should be
     * shown.
     */
    @SuppressWarnings("deprecation")
    private void setupSimplePreferencesScreen() {
        if (!isSimplePreferences(this)) {
            return;
        }

        addPreferencesFromResource(R.xml.pref_blank);

        // Add 'filters' preferences.
        PreferenceCategory fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.filters);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_filters);
        bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_log_level)));
        setupTagFilterPreference(this, findPreference(getString(R.string.pref_tag_filter)));
        sTagFilterPref = findPreference(getString(R.string.pref_tag_filter));

        // Add 'appearance' preferences.
        fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.appearance);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_appearance);

        // Add 'info' preferences.
        fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.information);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_info);
        setupOpenSourceInfoPreference(this, findPreference(getString(R.string.pref_info_open_source)));
        setupVersionPref(this, findPreference(getString(R.string.pref_version)));

    }

    private static void setupTagFilterPreference(final Activity activity, Preference preference) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        preference.setSummary(prefs.getString(activity.getString(R.string.pref_tag_filter), null));
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(activity, TagFilterListActivity.class);
                activity.startActivityForResult(intent, CODE_TAG_FILTER);
                return true;
            }
        });
    }

    private static void setupOpenSourceInfoPreference(final Activity activity, Preference preference) {
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                FragmentManager fm = activity.getFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                Fragment prev = fm.findFragmentByTag("dialog_licenses");
                if (prev != null) {
                    ft.remove(prev);
                }
                ft.addToBackStack(null);
                new OpenSourceLicensesDialog().show(ft, "dialog_licenses");
                return true;
            }
        });
    }

    private static void setupVersionPref(Activity activity, Preference preference) {
        try {
            String name = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
            preference.setSummary(activity.getString(R.string.version) + " " + name);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static class FilterPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.pref_filters);
            bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_log_level)));
            setupTagFilterPreference(getActivity(), findPreference(getString(R.string.pref_tag_filter)));
            sTagFilterPref = findPreference(getString(R.string.pref_tag_filter));
        }
    }

    public static class AppearancePreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.pref_appearance);
        }
    }

    public static class InfoPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.pref_info);
            setupOpenSourceInfoPreference(getActivity(), findPreference(getString(R.string.pref_info_open_source)));
            setupVersionPref(getActivity(), findPreference(getString(R.string.pref_version)));
        }
    }

    public static class OpenSourceLicensesDialog extends DialogFragment {

        public OpenSourceLicensesDialog() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            WebView webView = new WebView(getActivity());
            webView.loadUrl("file:///android_asset/licenses.html");

            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.open_source_licences)
                    .setView(webView)
                    .setPositiveButton(R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dialog.dismiss();
                                }
                            }
                    )
                    .create();
        }
    }

}
