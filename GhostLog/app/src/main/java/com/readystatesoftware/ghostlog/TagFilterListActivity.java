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

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class TagFilterListActivity extends ListActivity {

    private SharedPreferences mPrefs;
    private TreeSet<String> mTags;
    private ArrayAdapter<String> mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        readTags();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        setResult(RESULT_OK, (new Intent()).setAction(mAdapter.getItem(position)));
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_tag_filter_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:

                LayoutInflater factory = LayoutInflater.from(this);
                final View textEntryView = factory.inflate(R.layout.alert_dialog_edittext, null);
                final EditText nameEntry = (EditText) textEntryView.findViewById(R.id.name_edit);

                AlertDialog dlg = new AlertDialog.Builder(this)
                        .setTitle(R.string.new_tag_filter)
                        .setMessage(R.string.enter_log_tag)
                        .setView(textEntryView)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                if (!TextUtils.isEmpty(nameEntry.getText().toString().trim())) {
                                    mTags.add(nameEntry.getText().toString());
                                    writeTags();
                                    readTags();
                                }
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // cancel, do nothing
                            }
                        })
                        .create();

                dlg.show();

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void readTags() {
        Set<String> set = mPrefs.getStringSet(getString(R.string.pref_tag_filter_set), new HashSet<String>());
        mTags = new TreeSet<String>(set);
        ArrayList<String> data = new ArrayList<String>();
        data.add(getString(R.string.none));
        data.addAll(mTags);
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_activated_1, data);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        getListView().setMultiChoiceModeListener(new ModeCallback());
        setListAdapter(mAdapter);
    }

    private void writeTags() {
        Editor editor = mPrefs.edit();
        editor.putStringSet(getString(R.string.pref_tag_filter_set), mTags);
        editor.apply();
    }

    private class ModeCallback implements ListView.MultiChoiceModeListener {

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.activity_tag_filter_list_selection, menu);
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return true;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_delete:
                    ArrayList<String> forRemoval = new ArrayList<String>();
                    SparseBooleanArray checked = getListView().getCheckedItemPositions();
                    int i = 1;
                    for(String tag : mTags) {
                        if (checked.get(i)) {
                            forRemoval.add(tag);
                        }
                        i++;
                    }
                    for (String tag : forRemoval) {
                        mTags.remove(tag);
                    }
                    writeTags();
                    readTags();

                    mode.finish();
                    break;
            }
            return true;
        }

        public void onDestroyActionMode(ActionMode mode) {
        }

        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            final int checkedCount = getListView().getCheckedItemCount();
            switch (checkedCount) {
                case 0:
                    mode.setTitle(null);
                    break;
                default:
                    mode.setTitle(checkedCount + getString(R.string._selected));
                    break;
            }
        }

    }

}
