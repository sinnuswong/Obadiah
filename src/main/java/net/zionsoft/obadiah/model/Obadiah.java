/*
 * Obadiah - Simple and Easy-to-Use Bible Reader
 * Copyright (C) 2014 ZionSoft
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

package net.zionsoft.obadiah.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Obadiah {
    public static interface OnStringsLoadedListener {
        public void onStringsLoaded(String[] strings);
    }

    public static interface OnTranslationsLoadedListener {
        public void onTranslationsLoaded(TranslationInfo[] downloaded, TranslationInfo[] available);
    }

    public static interface OnTranslationDownloadListener {
        public void onTranslationDownloaded(String translation, boolean isSuccessful);

        public void onTranslationDownloadProgress(String translation, int progress);
    }

    public static interface OnTranslationRemovedListener {
        public void onTranslationRemoved(String translation, boolean isSuccessful);
    }

    public static interface OnVersesSearchedListener {
        public void onVersesSearched(Verse[] verses);
    }

    private static final int BOOK_COUNT = 66;
    private static final int[] CHAPTER_COUNT = {50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29,
            36, 10, 13, 10, 42, 150, 31, 12, 8, 66, 52, 5, 48, 12, 14, 3, 9, 1, 4, 7, 3, 3, 3, 2,
            14, 4, 28, 16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13, 5, 5, 3, 5, 1,
            1, 1, 22};

    private static Obadiah sInstance;

    private final DatabaseHelper mDatabaseHelper;

    private final LruCache<String, String[]> mBookNameCache;
    private final LruCache<String, String[]> mVerseCache;
    private String[] mDownloadedTranslations;

    public static void initialize(Context context) {
        if (sInstance == null) {
            synchronized (Obadiah.class) {
                if (sInstance == null)
                    sInstance = new Obadiah(context.getApplicationContext());
            }
        }
    }

    private Obadiah(Context context) {
        super();

        mDatabaseHelper = new DatabaseHelper(context);

        final long maxMemory = Runtime.getRuntime().maxMemory();
        mBookNameCache = createCache((int) (maxMemory / 16L));
        mVerseCache = createCache((int) (maxMemory / 8L));
    }

    private static LruCache<String, String[]> createCache(int cacheSize) {
        return new LruCache<String, String[]>(cacheSize) {
            @Override
            protected int sizeOf(String key, String[] verses) {
                // strings are UTF-16 encoded (with a length of one or two 16-bit code units)
                int length = 0;
                for (String verse : verses)
                    length += verse.length() * 4;
                return length;
            }
        };
    }

    public static Obadiah getInstance() {
        return sInstance;
    }

    public static int getBookCount() {
        return BOOK_COUNT;
    }

    public static int getChapterCount(int book) {
        return CHAPTER_COUNT[book];
    }

    public void clearCache() {
        mBookNameCache.evictAll();
        mVerseCache.evictAll();
    }

    public void loadTranslations(final OnTranslationsLoadedListener listener) {
        new AsyncTask<Void, Void, TranslationInfo[][]>() {
            @Override
            protected TranslationInfo[][] doInBackground(Void... params) {
                try {
                    if (mDownloadedTranslations == null) {
                        // this should not happen, but just in case
                        mDownloadedTranslations = getDownloadedTranslations();
                    }

                    final JSONArray replyArray = new JSONArray(
                            new String(NetworkHelper.get(NetworkHelper.TRANSLATIONS_LIST_URL), "UTF8"));
                    final int length = replyArray.length();
                    final TranslationInfo[] downloaded = new TranslationInfo[mDownloadedTranslations.length];
                    final TranslationInfo[] available = new TranslationInfo[length - mDownloadedTranslations.length];
                    int downloadedCounter = 0;
                    int availableCounter = 0;
                    for (int i = 0; i < length; ++i) {
                        final JSONObject translationObject = replyArray.getJSONObject(i);
                        final TranslationInfo translationInfo = new TranslationInfo(
                                translationObject.getString("name"), translationObject.getString("shortName"),
                                translationObject.getString("language"), translationObject.getInt("size"));

                        boolean isDownloaded = false;
                        for (String shortName : mDownloadedTranslations) {
                            if (translationInfo.shortName.equals(shortName)) {
                                isDownloaded = true;
                                break;
                            }
                        }
                        if (isDownloaded)
                            downloaded[downloadedCounter++] = translationInfo;
                        else
                            available[availableCounter++] = translationInfo;
                    }

                    return new TranslationInfo[][]{downloaded, available};
                } catch (Exception e) {
                    Analytics.trackException("Failed to load translations - " + e.getMessage());
                }
                return null;
            }

            @Override
            protected void onPostExecute(TranslationInfo[][] result) {
                if (result == null || result.length != 2)
                    listener.onTranslationsLoaded(null, null);
                else
                    listener.onTranslationsLoaded(result[0], result[1]);
            }
        }.execute();
    }

    private String[] getDownloadedTranslations() {
        synchronized (mDatabaseHelper) {
            SQLiteDatabase db = null;
            Cursor cursor = null;
            try {
                db = mDatabaseHelper.getReadableDatabase();
                if (db == null)
                    return null;
                cursor = db.query(true, DatabaseHelper.TABLE_BOOK_NAMES,
                        new String[]{DatabaseHelper.COLUMN_TRANSLATION_SHORT_NAME},
                        null, null, null, null, null, null);
                final int translationShortName = cursor.getColumnIndex(
                        DatabaseHelper.COLUMN_TRANSLATION_SHORT_NAME);
                int i = 0;
                String[] translations = new String[cursor.getCount()];
                while (cursor.moveToNext())
                    translations[i++] = cursor.getString(translationShortName);
                return translations;
            } finally {
                if (cursor != null)
                    cursor.close();
                if (db != null)
                    db.close();
            }
        }
    }

    public void loadDownloadedTranslations(final OnStringsLoadedListener listener) {
        if (mDownloadedTranslations != null) {
            listener.onStringsLoaded(mDownloadedTranslations);
            return;
        }

        new AsyncTask<Void, Void, String[]>() {
            @Override
            protected String[] doInBackground(Void... params) {
                return getDownloadedTranslations();
            }

            @Override
            protected void onPostExecute(String[] result) {
                mDownloadedTranslations = result;
                listener.onStringsLoaded(result);
            }
        }.execute();
    }

    public void loadBookNames(final String translationShortName, final OnStringsLoadedListener listener) {
        String[] bookNames = mBookNameCache.get(translationShortName);
        if (bookNames != null) {
            listener.onStringsLoaded(bookNames);
            return;
        }

        new AsyncTask<Void, Void, String[]>() {
            @Override
            protected String[] doInBackground(Void... params) {
                synchronized (mDatabaseHelper) {
                    SQLiteDatabase db = null;
                    try {
                        db = mDatabaseHelper.getReadableDatabase();
                        if (db == null)
                            return null;
                        return getBookNames(db, translationShortName);
                    } finally {
                        if (db != null)
                            db.close();
                    }
                }
            }

            @Override
            protected void onPostExecute(String[] result) {
                mBookNameCache.put(translationShortName, result);
                listener.onStringsLoaded(result);
            }
        }.execute();
    }

    private static String[] getBookNames(SQLiteDatabase db, String translationShortName) {
        Cursor cursor = null;
        try {
            cursor = db.query(DatabaseHelper.TABLE_BOOK_NAMES,
                    new String[]{DatabaseHelper.COLUMN_BOOK_NAME},
                    String.format("%s = ?", DatabaseHelper.COLUMN_TRANSLATION_SHORT_NAME),
                    new String[]{translationShortName}, null, null,
                    String.format("%s ASC", DatabaseHelper.COLUMN_BOOK_INDEX));
            final int bookName = cursor.getColumnIndex(DatabaseHelper.COLUMN_BOOK_NAME);
            int i = 0;
            String[] bookNames = new String[BOOK_COUNT];
            while (cursor.moveToNext())
                bookNames[i++] = cursor.getString(bookName);
            return bookNames;
        } finally {
            if (cursor != null)
                cursor.close();
        }
    }

    public void loadVerses(final String translationShortName, final int book, final int chapter,
                           final OnStringsLoadedListener listener) {
        final String key = buildVersesCacheKey(translationShortName, book, chapter);
        String[] verses = mVerseCache.get(key);
        if (verses != null) {
            listener.onStringsLoaded(verses);
            return;
        }

        new AsyncTask<Void, Void, String[]>() {
            @Override
            protected String[] doInBackground(Void... params) {
                synchronized (mDatabaseHelper) {
                    SQLiteDatabase db = null;
                    Cursor cursor = null;
                    try {
                        db = mDatabaseHelper.getReadableDatabase();
                        if (db == null)
                            return null;
                        cursor = db.query(translationShortName,
                                new String[]{DatabaseHelper.COLUMN_TEXT},
                                String.format("%s = ? AND %s = ?",
                                        DatabaseHelper.COLUMN_BOOK_INDEX, DatabaseHelper.COLUMN_CHAPTER_INDEX),
                                new String[]{Integer.toString(book), Integer.toString(chapter)},
                                null, null, String.format("%s ASC", DatabaseHelper.COLUMN_VERSE_INDEX)
                        );
                        final int verse = cursor.getColumnIndex(DatabaseHelper.COLUMN_TEXT);
                        int i = 0;
                        String[] verses = new String[cursor.getCount()];
                        while (cursor.moveToNext())
                            verses[i++] = cursor.getString(verse);
                        return verses;
                    } finally {
                        if (cursor != null)
                            cursor.close();
                        if (db != null)
                            db.close();
                    }
                }
            }

            @Override
            protected void onPostExecute(String[] result) {
                mVerseCache.put(key, result);
                listener.onStringsLoaded(result);
            }
        }.execute();
    }

    private static String buildVersesCacheKey(String translationShortName, int book, int chapter) {
        return String.format("%s-%d-%d", translationShortName, book, chapter);
    }

    public void searchVerses(final String translationShortName, final String keyword,
                             final OnVersesSearchedListener listener) {
        new AsyncTask<Void, Void, Verse[]>() {
            @Override
            protected Verse[] doInBackground(Void... params) {
                synchronized (mDatabaseHelper) {
                    SQLiteDatabase db = null;
                    Cursor cursor = null;
                    try {
                        db = mDatabaseHelper.getReadableDatabase();
                        if (db == null)
                            return null;
                        cursor = db.query(translationShortName,
                                new String[]{DatabaseHelper.COLUMN_BOOK_INDEX, DatabaseHelper.COLUMN_CHAPTER_INDEX,
                                        DatabaseHelper.COLUMN_VERSE_INDEX, DatabaseHelper.COLUMN_TEXT},
                                String.format("%s LIKE ?", DatabaseHelper.COLUMN_TEXT),
                                new String[]{String.format("%%%s%%", keyword.trim().replaceAll("\\s+", "%"))},
                                null, null, null
                        );
                        final int count = cursor.getCount();
                        if (count == 0)
                            return new Verse[0];

                        String[] bookNames = mBookNameCache.get(translationShortName);
                        if (bookNames == null) {
                            // this should not happen, but just in case
                            bookNames = getBookNames(db, translationShortName);
                            mBookNameCache.put(translationShortName, bookNames);
                        }

                        final int bookIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_BOOK_INDEX);
                        final int chapterIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_CHAPTER_INDEX);
                        final int verseIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_VERSE_INDEX);
                        final int verseText = cursor.getColumnIndex(DatabaseHelper.COLUMN_TEXT);
                        int i = 0;
                        final Verse[] verses = new Verse[count];
                        while (cursor.moveToNext()) {
                            final int book = cursor.getInt(bookIndex);
                            verses[i++] = new Verse(book, cursor.getInt(chapterIndex), cursor.getInt(verseIndex),
                                    bookNames[book], cursor.getString(verseText));
                        }
                        return verses;
                    } finally {
                        if (cursor != null)
                            cursor.close();
                        if (db != null)
                            db.close();
                    }
                }
            }

            @Override
            protected void onPostExecute(Verse[] result) {
                listener.onVersesSearched(result);
            }
        }.execute();
    }

    public void downloadTranslation(final String translationShortName, final OnTranslationDownloadListener listener) {
        new AsyncTask<Void, Integer, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                synchronized (mDatabaseHelper) {
                    ZipInputStream zis = null;
                    SQLiteDatabase db = null;
                    try {
                        db = mDatabaseHelper.getWritableDatabase();
                        if (db == null)
                            return false;
                        db.beginTransaction();

                        // creates a translation table
                        db.execSQL(String.format("CREATE TABLE %s (%s INTEGER NOT NULL, %s INTEGER NOT NULL, %s INTEGER NOT NULL, %s TEXT NOT NULL);",
                                translationShortName, DatabaseHelper.COLUMN_BOOK_INDEX, DatabaseHelper.COLUMN_CHAPTER_INDEX,
                                DatabaseHelper.COLUMN_VERSE_INDEX, DatabaseHelper.COLUMN_TEXT));
                        db.execSQL(String.format("CREATE INDEX INDEX_%s ON %s (%s, %s, %s);",
                                translationShortName, translationShortName, DatabaseHelper.COLUMN_BOOK_INDEX,
                                DatabaseHelper.COLUMN_CHAPTER_INDEX, DatabaseHelper.COLUMN_VERSE_INDEX));

                        zis = new ZipInputStream(NetworkHelper.getStream(String.format(
                                NetworkHelper.TRANSLATION_URL_TEMPLATE, URLEncoder.encode(translationShortName, "UTF-8"))));

                        final byte buffer[] = new byte[2048];
                        final ByteArrayOutputStream os = new ByteArrayOutputStream();
                        final ContentValues versesValues = new ContentValues(4);
                        int downloaded = 0;
                        int read;
                        ZipEntry entry;
                        while ((entry = zis.getNextEntry()) != null) {
                            os.reset();
                            while ((read = zis.read(buffer, 0, 2048)) != -1)
                                os.write(buffer, 0, read);
                            final byte[] bytes = os.toByteArray();
                            String fileName = entry.getName();
                            fileName = fileName.substring(0, fileName.length() - 5); // removes the trailing ".json"
                            if (fileName.equals("books")) {
                                // writes the book names table

                                final ContentValues bookNamesValues = new ContentValues(3);
                                bookNamesValues.put(DatabaseHelper.COLUMN_TRANSLATION_SHORT_NAME, translationShortName);

                                final JSONObject booksInfoObject = new JSONObject(new String(bytes, "UTF8"));
                                final JSONArray booksArray = booksInfoObject.getJSONArray("books");
                                for (int i = 0; i < Obadiah.getBookCount(); ++i) {
                                    bookNamesValues.put(DatabaseHelper.COLUMN_BOOK_INDEX, i);
                                    bookNamesValues.put(DatabaseHelper.COLUMN_BOOK_NAME, booksArray.getString(i));
                                    db.insert(DatabaseHelper.TABLE_BOOK_NAMES, null, bookNamesValues);
                                }
                            } else {
                                // writes the verses

                                final String[] parts = fileName.split("-");
                                final int bookIndex = Integer.parseInt(parts[0]);
                                final int chapterIndex = Integer.parseInt(parts[1]);
                                versesValues.put(DatabaseHelper.COLUMN_BOOK_INDEX, bookIndex);
                                versesValues.put(DatabaseHelper.COLUMN_CHAPTER_INDEX, chapterIndex);

                                final JSONObject jsonObject = new JSONObject(new String(bytes, "UTF8"));
                                final JSONArray paragraphArray = jsonObject.getJSONArray("verses");
                                final int paragraphCount = paragraphArray.length();
                                for (int verseIndex = 0; verseIndex < paragraphCount; ++verseIndex) {
                                    versesValues.put(DatabaseHelper.COLUMN_VERSE_INDEX, verseIndex);
                                    versesValues.put(DatabaseHelper.COLUMN_TEXT, paragraphArray.getString(verseIndex));
                                    db.insert(translationShortName, null, versesValues);
                                }
                            }

                            // broadcasts progress
                            publishProgress(++downloaded / 12);
                        }

                        db.setTransactionSuccessful();

                        return true;
                    } catch (Exception e) {
                        Analytics.trackException("Failed to download translations - " + e.getMessage());
                        return false;
                    } finally {
                        if (db != null) {
                            if (db.inTransaction())
                                db.endTransaction();
                            db.close();
                        }
                        if (zis != null) {
                            try {
                                zis.close();
                            } catch (IOException e) {
                                // we can't do much here
                            }
                        }
                    }
                }
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                listener.onTranslationDownloadProgress(translationShortName, values[0]);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                Analytics.trackTranslationDownload(translationShortName, result);

                final String[] downloadedTranslations = new String[mDownloadedTranslations.length + 1];
                System.arraycopy(mDownloadedTranslations, 0, downloadedTranslations, 0, mDownloadedTranslations.length);
                downloadedTranslations[mDownloadedTranslations.length] = translationShortName;

                mDownloadedTranslations = downloadedTranslations;

                listener.onTranslationDownloaded(translationShortName, result);
            }
        }.execute();
    }

    public void removeTranslation(final String translationShortName, final OnTranslationRemovedListener listener) {
        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                synchronized (mDatabaseHelper) {
                    SQLiteDatabase db = null;
                    try {
                        db = mDatabaseHelper.getWritableDatabase();
                        if (db == null)
                            return false;

                        db.beginTransaction();

                        // deletes the translation table
                        db.execSQL(String.format("DROP TABLE IF EXISTS %s", translationShortName));

                        // deletes the book names
                        db.delete(DatabaseHelper.TABLE_BOOK_NAMES,
                                String.format("%s = ?", DatabaseHelper.COLUMN_TRANSLATION_SHORT_NAME),
                                new String[]{translationShortName});

                        db.setTransactionSuccessful();

                        return true;
                    } finally {
                        if (db != null) {
                            if (db.inTransaction())
                                db.endTransaction();
                            db.close();
                        }
                    }
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                Analytics.trackTranslationRemoval(translationShortName, result);

                final String[] downloadedTranslations = new String[mDownloadedTranslations.length - 1];
                int i = 0;
                for (String translation : mDownloadedTranslations) {
                    if (!translation.equals(translationShortName))
                        downloadedTranslations[i++] = translation;
                }
                mDownloadedTranslations = downloadedTranslations;

                listener.onTranslationRemoved(translationShortName, result);
            }
        }.execute();
    }
}
