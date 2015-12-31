/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2015 ZionSoft
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

package net.zionsoft.obadiah.biblereading.toolbar;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.zionsoft.obadiah.model.datamodel.BibleReadingModel;
import net.zionsoft.obadiah.model.domain.Verse;
import net.zionsoft.obadiah.mvp.MVPPresenter;

import java.util.List;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class ToolbarPresenter extends MVPPresenter<ToolbarView> {
    private final BibleReadingModel bibleReadingModel;
    private CompositeSubscription subscription;

    public ToolbarPresenter(BibleReadingModel bibleReadingModel) {
        this.bibleReadingModel = bibleReadingModel;
    }

    @Override
    protected void onViewTaken() {
        super.onViewTaken();

        getSubscription().add(bibleReadingModel.observeCurrentTranslation()
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        // do nothing
                    }

                    @Override
                    public void onNext(String translation) {
                        loadBookNames(translation);
                    }
                }));
        getSubscription().add(bibleReadingModel.observeCurrentReadingProgress()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Verse.Index>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        // do nothing
                    }

                    @Override
                    public void onNext(Verse.Index index) {
                        final ToolbarView v = getView();
                        if (v != null) {
                            v.onReadingProgressUpdated(index);
                        }
                    }
                }));
    }

    @NonNull
    private CompositeSubscription getSubscription() {
        if (subscription == null) {
            subscription = new CompositeSubscription();
        }
        return subscription;
    }

    private void loadBookNames(String translation) {
        getSubscription().add(bibleReadingModel.loadBookNames(translation)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<String>>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        // do nothing
                    }

                    @Override
                    public void onNext(List<String> bookNames) {
                        final ToolbarView v = getView();
                        if (v != null) {
                            // if the list is empty, it means the requested translation is not
                            // installed yet, do nothing
                            if (bookNames.size() > 0) {
                                v.onBookNamesLoaded(bookNames);
                            }
                        }
                    }
                }));
    }

    @Override
    protected void onViewDropped() {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }

        super.onViewDropped();
    }

    @Nullable
    String loadCurrentTranslation() {
        return bibleReadingModel.loadCurrentTranslation();
    }

    void saveCurrentTranslation(String translation) {
        bibleReadingModel.saveCurrentTranslation(translation);
    }

    int loadCurrentBook() {
        return bibleReadingModel.loadCurrentBook();
    }

    int loadCurrentChapter() {
        return bibleReadingModel.loadCurrentChapter();
    }

    void loadTranslations() {
        getSubscription().add(bibleReadingModel.loadTranslations()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<List<String>>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        // do nothing
                    }

                    @Override
                    public void onNext(List<String> translations) {
                        final ToolbarView v = getView();
                        if (v != null) {
                            if (translations.size() > 0) {
                                v.onTranslationsLoaded(translations);
                            }
                        }
                    }
                }));
    }

    void loadBookNamesForCurrentTranslation() {
        loadBookNames(bibleReadingModel.loadCurrentTranslation());
    }
}