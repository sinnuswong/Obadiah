/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2013 ZionSoft
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.zionsoft.obadiah;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import net.zionsoft.obadiah.bible.TranslationInfo;
import net.zionsoft.obadiah.bible.TranslationManager;
import net.zionsoft.obadiah.util.SettingsManager;

import java.util.ArrayList;
import java.util.List;

public class TranslationSelectionActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.translationselection_activity);

        mSettingsManager = new SettingsManager(this);
        mTranslationManager = new TranslationManager(this);
        mSelectedTranslationShortName = getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE)
                .getString(Constants.PREF_KEY_LAST_READ_TRANSLATION, null);

        // initializes views
        mLoadingSpinner = findViewById(R.id.translation_selection_loading_spinner);

        // initializes list view showing installed translations
        mTranslationListAdapter = new TranslationSelectionListAdapter(this, mSettingsManager);
        mTranslationListAdapter.setSelectedTranslation(mSelectedTranslationShortName);
        mTranslationListView = (ListView) findViewById(R.id.translation_list_view);
        mTranslationListView.setAdapter(mTranslationListAdapter);
        mTranslationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSelectedTranslationShortName = mTranslationListAdapter.getItem(position).shortName;
                finish();
            }
        });
        mTranslationListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final String selected = mTranslationListAdapter.getItem(position).shortName;
                if (selected.equals(mSelectedTranslationShortName))
                    return false;

                final Resources resources = TranslationSelectionActivity.this.getResources();
                final CharSequence[] items = {resources.getText(R.string.text_delete)};
                final AlertDialog.Builder contextMenuDialogBuilder = new AlertDialog.Builder(
                        TranslationSelectionActivity.this);
                contextMenuDialogBuilder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: // delete
                                new AlertDialog.Builder(TranslationSelectionActivity.this)
                                        .setMessage(resources.getText(R.string.text_delete_confirm))
                                        .setCancelable(true)
                                        .setPositiveButton(android.R.string.ok,
                                                new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {
                                                        removeTranslation(selected);
                                                    }
                                                })
                                        .setNegativeButton(android.R.string.cancel, null)
                                        .create().show();
                                break;
                        }
                    }
                });
                contextMenuDialogBuilder.create().show();

                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSettingsManager.refresh();
        final int backgroundColor = mSettingsManager.backgroundColor();
        mTranslationListView.setBackgroundColor(backgroundColor);
        mTranslationListView.setCacheColorHint(backgroundColor);

        populateUi();
    }

    @Override
    protected void onPause() {
        getSharedPreferences(Constants.PREF_NAME, MODE_PRIVATE).edit()
                .putString(Constants.PREF_KEY_LAST_READ_TRANSLATION, mSelectedTranslationShortName)
                .commit();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_translation_selection, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_download:
                startTranslationDownloadActivity();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void populateUi() {
        new AsyncTask<Void, Void, List<TranslationInfo>>() {
            @Override
            protected void onPreExecute() {
                mLoadingSpinner.setVisibility(View.VISIBLE);
                mTranslationListView.setVisibility(View.GONE);
            }

            @Override
            protected List<TranslationInfo> doInBackground(Void... params) {
                final TranslationInfo[] translations = mTranslationManager.translations();
                int installedTranslationCount = translations == null ? 0 : translations.length;
                if (installedTranslationCount > 0) {
                    for (TranslationInfo translationInfo : translations) {
                        if (!translationInfo.installed)
                            --installedTranslationCount;
                    }
                }
                if (installedTranslationCount == 0)
                    return null;

                final List<TranslationInfo> installedTranslations
                        = new ArrayList<TranslationInfo>(installedTranslationCount);
                for (TranslationInfo translationInfo : translations) {
                    if (translationInfo.installed)
                        installedTranslations.add(translationInfo);
                }
                return installedTranslations;
            }

            @Override
            protected void onPostExecute(List<TranslationInfo> translations) {
                if (translations == null) {
                    if (mFirstTime) {
                        startTranslationDownloadActivity();
                        mFirstTime = false;
                    }
                    return;
                }

                Animator.fadeOut(mLoadingSpinner);
                Animator.fadeIn(mTranslationListView);

                // 1st time resumed from TranslationDownloadActivity with installed translation
                if (mSelectedTranslationShortName == null)
                    mSelectedTranslationShortName = translations.get(0).shortName;

                mTranslationListAdapter.setTranslations(translations);
            }
        }.execute();
    }

    private void removeTranslation(String translationShortName) {
        new AsyncTask<String, Void, Void>() {
            protected void onPreExecute() {
                // running in the main thread

                mProgressDialog = new ProgressDialog(TranslationSelectionActivity.this);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setMessage(getText(R.string.text_deleting));
                mProgressDialog.show();
            }

            protected Void doInBackground(String... params) {
                // running in the worker thread

                mTranslationManager.removeTranslation(params[0]);
                return null;
            }

            protected void onPostExecute(Void result) {
                // running in the main thread

                populateUi();
                mProgressDialog.cancel();
                Toast.makeText(TranslationSelectionActivity.this,
                        R.string.text_deleted, Toast.LENGTH_SHORT).show();
            }

            private ProgressDialog mProgressDialog;
        }.execute(translationShortName);
    }

    private void startTranslationDownloadActivity() {
        // checks connectivity
        final ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            Toast.makeText(TranslationSelectionActivity.this, R.string.text_no_network, Toast.LENGTH_LONG).show();
            return;
        }

        // HTTP connection reuse was buggy before Froyo (i.e. Android 2.2, API Level 8)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO)
            System.setProperty("http.keepAlive", "false");

        startActivity(new Intent(TranslationSelectionActivity.this, TranslationDownloadActivity.class));
    }

    private boolean mFirstTime = true;

    private ListView mTranslationListView;
    private View mLoadingSpinner;

    private SettingsManager mSettingsManager;
    private String mSelectedTranslationShortName;
    private TranslationManager mTranslationManager;
    private TranslationSelectionListAdapter mTranslationListAdapter;
}
