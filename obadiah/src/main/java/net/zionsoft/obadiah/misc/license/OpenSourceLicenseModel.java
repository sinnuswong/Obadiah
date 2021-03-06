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

package net.zionsoft.obadiah.misc.license;

import android.content.Context;

import com.google.android.gms.common.GoogleApiAvailability;

import net.zionsoft.obadiah.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import rx.Observable;

class OpenSourceLicenseModel {
    private final Context applicationContext;

    OpenSourceLicenseModel(Context context) {
        this.applicationContext = context.getApplicationContext();
    }

    Observable<List<String>> loadLicense() {
        return Observable.fromCallable(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                final List<String> licenses = new ArrayList<>();
                licenses.add(GoogleApiAvailability.getInstance()
                        .getOpenSourceSoftwareLicenseInfo(applicationContext));
                licenses.addAll(Arrays.asList(applicationContext
                        .getResources().getStringArray(R.array.licenses)));
                return licenses;
            }
        });
    }
}
