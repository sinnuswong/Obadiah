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

package net.zionsoft.obadiah.translations;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;

import net.zionsoft.obadiah.model.datamodel.BibleReadingModel;
import net.zionsoft.obadiah.model.datamodel.Settings;
import net.zionsoft.obadiah.model.domain.TranslationInfo;
import net.zionsoft.obadiah.mvp.BasePresenter;
import net.zionsoft.obadiah.utils.RxHelper;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.subscriptions.CompositeSubscription;

class TranslationManagementPresenter extends BasePresenter<TranslationManagementView>
        implements AdsModel.OnAdsRemovalPurchasedListener {
    private final AdsModel adsModel;
    final BibleReadingModel bibleReadingModel;
    private final TranslationManagementModel translationManagementModel;

    private CompositeSubscription subscription = new CompositeSubscription();
    Subscription removeTranslationSubscription;
    Subscription fetchTranslationSubscription;

    TranslationManagementPresenter(AdsModel adsModel, BibleReadingModel bibleReadingModel,
                                   TranslationManagementModel translationManagementModel, Settings settings) {
        super(settings);
        this.adsModel = adsModel;
        this.bibleReadingModel = bibleReadingModel;
        this.translationManagementModel = translationManagementModel;
    }

    @Override
    protected void onViewTaken() {
        super.onViewTaken();
        if (subscription == null) {
            subscription = new CompositeSubscription();
        }
    }

    @Override
    protected void onViewDropped() {
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }

        super.onViewDropped();
    }

    String loadCurrentTranslation() {
        return bibleReadingModel.loadCurrentTranslation();
    }

    void saveCurrentTranslation(String translation) {
        bibleReadingModel.saveCurrentTranslation(translation);
    }

    void loadTranslations(boolean forceRefresh) {
        subscription.add(translationManagementModel.loadTranslations(forceRefresh)
                .compose(RxHelper.<Translations>applySchedulers())
                .subscribe(new Subscriber<Translations>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        final TranslationManagementView v = getView();
                        if (v != null) {
                            v.onTranslationLoadFailed();
                        }
                    }

                    @Override
                    public void onNext(Translations translations) {
                        final TranslationManagementView v = getView();
                        if (v != null) {
                            v.onTranslationLoaded(translations);
                        }
                    }
                }));
    }

    void removeTranslation(final TranslationInfo translation) {
        if (removeTranslationSubscription != null) {
            return;
        }

        removeTranslationSubscription = translationManagementModel.removeTranslation(translation)
                .doOnTerminate(new Action0() {
                    @Override
                    public void call() {
                        removeTranslationSubscription = null;
                    }
                }).compose(RxHelper.<Void>applySchedulers())
                .subscribe(new Subscriber<Void>() {
                    @Override
                    public void onCompleted() {
                        final TranslationManagementView v = getView();
                        if (v != null) {
                            v.onTranslationRemoved(translation);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        final TranslationManagementView v = getView();
                        if (v != null) {
                            v.onTranslationRemovalFailed(translation);
                        }
                    }

                    @Override
                    public void onNext(Void aVoid) {
                        // won't reach here
                    }
                });
    }

    void cancelRemoveTranslation() {
        if (removeTranslationSubscription != null) {
            removeTranslationSubscription.unsubscribe();
            removeTranslationSubscription = null;
        }
    }

    void fetchTranslation(final TranslationInfo translation) {
        if (fetchTranslationSubscription != null) {
            return;
        }

        fetchTranslationSubscription = translationManagementModel.fetchTranslation(translation)
                .doOnTerminate(new Action0() {
                    @Override
                    public void call() {
                        fetchTranslationSubscription = null;
                    }
                }).compose(RxHelper.<Integer>applySchedulers())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {
                        if (TextUtils.isEmpty(bibleReadingModel.loadCurrentTranslation())) {
                            bibleReadingModel.saveCurrentTranslation(translation.shortName());
                        }

                        final TranslationManagementView v = getView();
                        if (v != null) {
                            v.onTranslationDownloaded(translation);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        final TranslationManagementView v = getView();
                        if (v != null) {
                            v.onTranslationDownloadFailed(translation);
                        }
                    }

                    @Override
                    public void onNext(Integer progress) {
                        final TranslationManagementView v = getView();
                        if (v != null) {
                            v.onTranslationDownloadProgressed(translation, progress);
                        }
                    }
                });
    }

    void cancelFetchTranslation() {
        if (fetchTranslationSubscription != null) {
            fetchTranslationSubscription.unsubscribe();
            fetchTranslationSubscription = null;
        }
    }

    void loadAdsStatus() {
        subscription.add(adsModel.shouldHideAds()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {
                        // do nothing
                    }

                    @Override
                    public void onError(Throwable e) {
                        final TranslationManagementView v = getView();
                        if (v != null) {
                            v.showAds();
                        }
                    }

                    @Override
                    public void onNext(Boolean shouldHideAds) {
                        final TranslationManagementView v = getView();
                        if (v != null) {
                            if (shouldHideAds) {
                                v.hideAds();
                            } else {
                                v.showAds();
                            }
                        }
                    }
                }));
    }

    void removeAds(Activity activity) {
        adsModel.purchaseAdsRemoval(activity, this);
    }

    boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        return adsModel.handleActivityResult(requestCode, resultCode, data);
    }

    void cleanup() {
        adsModel.cleanup();
    }

    @Override
    public void onAdsRemovalPurchased(boolean purchased) {
        final TranslationManagementView v = getView();
        if (v != null) {
            if (purchased) {
                v.hideAds();
            } else {
                v.showAds();
            }
        }
    }
}
