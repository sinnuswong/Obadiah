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

package net.zionsoft.obadiah.search;

import android.content.Context;
import android.provider.SearchRecentSuggestions;

import net.zionsoft.obadiah.model.datamodel.BibleReadingModel;
import net.zionsoft.obadiah.model.domain.VerseSearchResult;

import java.util.List;

import rx.Observable;
import rx.functions.Func0;

class SearchModel {
    private final BibleReadingModel bibleReadingModel;
    final SearchRecentSuggestions recentSearches;

    SearchModel(Context context, BibleReadingModel bibleReadingModel) {
        this.bibleReadingModel = bibleReadingModel;
        this.recentSearches = new SearchRecentSuggestions(context,
                RecentSearchProvider.AUTHORITY, RecentSearchProvider.MODE);
    }

    Observable<List<VerseSearchResult>> search(String translation, String query) {
        recentSearches.saveRecentQuery(query, null);
        return bibleReadingModel.search(translation, query);
    }

    Observable<Void> clearSearchHistory() {
        return Observable.defer(new Func0<Observable<Void>>() {
            @Override
            public Observable<Void> call() {
                recentSearches.clearHistory();
                return Observable.empty();
            }
        });
    }
}
