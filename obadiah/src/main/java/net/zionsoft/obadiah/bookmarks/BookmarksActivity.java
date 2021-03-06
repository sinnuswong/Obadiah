/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2016 ZionSoft
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

package net.zionsoft.obadiah.bookmarks;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.view.View;

import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.analytics.Analytics;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.Bookmark;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.ui.utils.AnimationHelper;
import net.zionsoft.obadiah.ui.utils.BaseRecyclerViewActivity;
import net.zionsoft.obadiah.ui.utils.DialogHelper;

import java.util.List;

import javax.inject.Inject;

public class BookmarksActivity extends BaseRecyclerViewActivity implements BookmarksView {
    @NonNull
    public static Intent newStartIntent(Context context) {
        return new Intent(context, BookmarksActivity.class);
    }

    @Inject
    BookmarksPresenter bookmarksPresenter;

    private BookmarksListAdapter bookmarksListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final FragmentManager fm = getSupportFragmentManager();
        BookmarksComponentFragment componentFragment = (BookmarksComponentFragment)
                fm.findFragmentByTag(BookmarksComponentFragment.FRAGMENT_TAG);
        if (componentFragment == null) {
            componentFragment = BookmarksComponentFragment.newInstance();
            fm.beginTransaction()
                    .add(componentFragment, BookmarksComponentFragment.FRAGMENT_TAG)
                    .commitNow();
        }
        componentFragment.getComponent().inject(this);

        final View rootView = getWindow().getDecorView();
        final Settings settings = bookmarksPresenter.getSettings();
        rootView.setKeepScreenOn(settings.keepScreenOn());
        rootView.setBackgroundColor(settings.getBackgroundColor());

        toolbar.setTitle(R.string.activity_bookmarks);

        bookmarksListAdapter = new BookmarksListAdapter(this, bookmarksPresenter.getSettings());
        recyclerView.setAdapter(bookmarksListAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bookmarksPresenter.takeView(this);
        loadBookmarks();
    }

    void loadBookmarks() {
        bookmarksPresenter.loadBookmarks();
    }

    @Override
    protected void onStop() {
        bookmarksPresenter.dropView();
        super.onStop();
    }

    @Override
    public void onBookmarksLoaded(List<Bookmark> bookmarks, List<Verse> verses) {
        AnimationHelper.fadeOut(loadingSpinner);
        AnimationHelper.fadeIn(recyclerView);

        bookmarksListAdapter.setBookmarks(bookmarks, verses);
    }

    @Override
    public void onBookmarksLoadFailed() {
        DialogHelper.showDialog(this, false, R.string.error_failed_to_load,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        loadBookmarks();
                    }
                }, null);
    }

    @Override
    protected void onChildClicked(int position) {
        final Verse verse = bookmarksListAdapter.getItem(position);
        if (verse != null) {
            bookmarksPresenter.saveReadingProgress(verse.verseIndex);
            Analytics.trackEvent(Analytics.CATEGORY_BOOKMARKS, Analytics.BOOKMARKS_ACTION_OPENED);
            finish();
        }
    }
}
