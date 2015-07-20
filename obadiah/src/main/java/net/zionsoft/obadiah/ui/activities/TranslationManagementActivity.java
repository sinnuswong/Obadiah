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

package net.zionsoft.obadiah.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import net.zionsoft.obadiah.App;
import net.zionsoft.obadiah.R;
import net.zionsoft.obadiah.model.InAppBillingHelper;
import net.zionsoft.obadiah.model.Settings;
import net.zionsoft.obadiah.model.analytics.Analytics;

import javax.inject.Inject;

import butterknife.Bind;

public class TranslationManagementActivity extends BaseAppCompatActivity {
    private static final String KEY_MESSAGE_TYPE = "net.zionsoft.obadiah.ui.activities.TranslationManagementActivity.KEY_MESSAGE_TYPE";

    public static Intent newStartReorderToTopIntent(Context context, String messageType) {
        final Intent startIntent = newStartIntent(context).putExtra(KEY_MESSAGE_TYPE, messageType);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // there's some horrible issue with FLAG_ACTIVITY_REORDER_TO_FRONT for KitKat and above
            // ref. https://code.google.com/p/android/issues/detail?id=63570
            startIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        }
        return startIntent;
    }

    public static Intent newStartIntent(Context context) {
        return new Intent(context, TranslationManagementActivity.class);
    }

    @Inject
    Settings settings;

    @Bind(R.id.ad_view)
    AdView adView;

    private MenuItem removeAdsMenuItem;

    private InAppBillingHelper inAppBillingHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.get(this).getInjectionComponent().inject(this);

        initializeUi();
        initializeInAppBillingHelper();
        checkDeepLink();
    }

    private void initializeUi() {
        setContentView(R.layout.activity_translation_management);

        final View rootView = getWindow().getDecorView();
        rootView.setBackgroundColor(settings.getBackgroundColor());
        rootView.setKeepScreenOn(settings.keepScreenOn());
    }

    private void initializeInAppBillingHelper() {
        inAppBillingHelper = new InAppBillingHelper();
        inAppBillingHelper.initialize(this, new InAppBillingHelper.OnInitializationFinishedListener() {
            @Override
            public void onInitializationFinished(boolean isSuccessful) {
                if (isSuccessful) {
                    inAppBillingHelper.loadAdsRemovalState(new InAppBillingHelper.OnAdsRemovalStateLoadedListener() {
                        @Override
                        public void onAdsRemovalStateLoaded(boolean isRemoved) {
                            if (isRemoved)
                                hideAds();
                            else
                                showAds();
                        }
                    });
                } else {
                    showAds();

                    // billing can't be initialized, then makes no sense to show the menu item
                    if (removeAdsMenuItem != null)
                        removeAdsMenuItem.setVisible(false);
                }
            }
        });
    }

    private void checkDeepLink() {
        final Intent startIntent = getIntent();
        final String messageType = startIntent.getStringExtra(KEY_MESSAGE_TYPE);
        if (TextUtils.isEmpty(messageType)) {
            return;
        }
        Analytics.trackNotificationEvent("notification_opened", messageType);
    }

    private void hideAds() {
        if (removeAdsMenuItem != null)
            removeAdsMenuItem.setVisible(false);

        adView.setVisibility(View.GONE);
    }

    private void showAds() {
        if (removeAdsMenuItem != null)
            removeAdsMenuItem.setVisible(true);

        adView.setVisibility(View.VISIBLE);
        adView.loadAd(new AdRequest.Builder()
                .addKeyword("bible").addKeyword("jesus").addKeyword("christian")
                .build());
    }

    @Override
    protected void onResume() {
        super.onResume();

        adView.resume();
    }

    @Override
    protected void onPause() {
        adView.pause();

        super.onPause();
    }

    @Override
    public void onDestroy() {
        adView.destroy();
        inAppBillingHelper.cleanup();

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_translation_management, menu);
        removeAdsMenuItem = menu.findItem(R.id.action_remove_ads);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_remove_ads:
                removeAds();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void removeAds() {
        Analytics.trackUIEvent("remove_ads");

        inAppBillingHelper.purchaseAdsRemoval(new InAppBillingHelper.OnAdsRemovalPurchasedListener() {
            @Override
            public void onAdsRemovalPurchased(boolean isSuccessful) {
                if (isSuccessful)
                    hideAds();

                // TODO error handling
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!inAppBillingHelper.handleActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }
}
