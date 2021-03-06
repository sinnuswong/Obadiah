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

package net.zionsoft.obadiah.notification;

import android.support.annotation.NonNull;

import com.squareup.moshi.Json;

import net.zionsoft.obadiah.model.domain.VerseIndex;

public class PushAttrVerseIndex {
    @Json(name = "book")
    public final int book;

    @Json(name = "chapter")
    public final int chapter;

    @Json(name = "verse")
    public final int verse;

    public PushAttrVerseIndex(int book, int chapter, int verse) {
        this.book = book;
        this.chapter = chapter;
        this.verse = verse;
    }

    @NonNull
    public VerseIndex toVerseIndex() {
        return VerseIndex.create(book, chapter, verse);
    }
}
