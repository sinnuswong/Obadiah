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

package net.zionsoft.obadiah.model.domain;

import android.content.ContentValues;
import android.database.Cursor;

import com.google.auto.value.AutoValue;

import net.zionsoft.auto.droid.ColumnAdapter;
import net.zionsoft.auto.droid.ColumnName;

@AutoValue
public abstract class Note {
    public static class ColumnNames {
        public static final String NOTE = "COLUMN_NOTE";
        public static final String TIMESTAMP = "COLUMN_TIMESTAMP";
    }

    @ColumnAdapter(VerseIndex.class)
    public abstract VerseIndex verseIndex();

    @ColumnName(ColumnNames.NOTE)
    public abstract String note();

    @ColumnName(ColumnNames.TIMESTAMP)
    public abstract long timestamp();

    public abstract ContentValues toContentValues(ContentValues contentValues);

    public static Note create(VerseIndex verseIndex, String note, long timestamp) {
        return new AutoValue_Note(verseIndex, note, timestamp);
    }

    public static Note create(Cursor cursor) {
        return AutoValue_Note.createFromCursor(cursor);
    }
}
