package net.zionsoft.obadiah;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class TranslationManager
{
    public TranslationManager(Context context)
    {
        super();
        m_translationsDatabaseHelper = new TranslationsDatabaseHelper(context);
    }

    // public void addTranslations(TranslationInfo[] translations)
    // {
    // if (translations == null)
    // throw new NullPointerException();
    // if (translations.length == 0)
    // throw new IllegalArgumentException();
    //
    // SQLiteDatabase db = m_translationsDatabaseOpenHelper.getWritableDatabase();
    // ContentValues values = new ContentValues(5);
    // values.put(COLUMN_INSTALLED, 0);
    // for (TranslationInfo translationInfo : translations) {
    // values.put(COLUMN_PATH, translationInfo.path);
    // values.put(COLUMN_TRANSLATION_NAME, translationInfo.name);
    // values.put(COLUMN_TRANSLATION_SHORTNAME, translationInfo.shortName);
    // values.put(COLUMN_DOWNLOAD_SIZE, translationInfo.size);
    // db.insert(TABLE_TRANSLATIONS, null, values);
    // }
    // }

    // translations before version 1.5.0 uses the old format
    public void convertFromOldFormat()
    {
        BibleReader oldReader = BibleReader.getInstance();
        TranslationInfo[] installedTranslations = oldReader.installedTranslations();
        if (installedTranslations == null || installedTranslations.length == 0)
            return;

        SQLiteDatabase db = m_translationsDatabaseHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues versesValues = new ContentValues(4);
            ContentValues bookNamesValues = new ContentValues(3);
            ContentValues translationInfoValues = new ContentValues(5);
            translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_INSTALLED, 1);
            for (TranslationInfo translationInfo : installedTranslations) {
                // creates a translation table
                db.execSQL("CREATE TABLE " + translationInfo.shortName + " ("
                        + TranslationsDatabaseHelper.COLUMN_BOOK_INDEX + " INTEGER NOT NULL, "
                        + TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX + " INTEGER NOT NULL, "
                        + TranslationsDatabaseHelper.COLUMN_VERSE_INDEX + " INTEGER NOT NULL, "
                        + TranslationsDatabaseHelper.COLUMN_TEXT + " TEXT NOT NULL);");
                db.execSQL("CREATE INDEX TRANSLATIONS_INDEX ON " + translationInfo.shortName + " ("
                        + TranslationsDatabaseHelper.COLUMN_BOOK_INDEX + ", "
                        + TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX + ", "
                        + TranslationsDatabaseHelper.COLUMN_VERSE_INDEX + ");");

                bookNamesValues.put(TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME, translationInfo.shortName);

                oldReader.selectTranslation(translationInfo.path);
                for (int bookIndex = 0; bookIndex < 66; ++bookIndex) {
                    // writes verses
                    for (int chapterIndex = 0; chapterIndex < oldReader.chapterCount(bookIndex); ++chapterIndex) {
                        String[] texts = oldReader.verses(bookIndex, chapterIndex);
                        int verseIndex = 0;
                        for (String text : texts) {
                            versesValues.put(TranslationsDatabaseHelper.COLUMN_BOOK_INDEX, bookIndex);
                            versesValues.put(TranslationsDatabaseHelper.COLUMN_CHAPTER_INDEX, chapterIndex);
                            versesValues.put(TranslationsDatabaseHelper.COLUMN_VERSE_INDEX, verseIndex++);
                            versesValues.put(TranslationsDatabaseHelper.COLUMN_TEXT, text);
                            db.insert(translationInfo.shortName, null, versesValues);
                        }
                    }

                    // writes book name
                    bookNamesValues.put(TranslationsDatabaseHelper.COLUMN_BOOK_INDEX, Integer.toString(bookIndex));
                    bookNamesValues.put(TranslationsDatabaseHelper.COLUMN_BOOK_NAME,
                            translationInfo.bookName[bookIndex]);
                    db.insert(TranslationsDatabaseHelper.TABLE_BOOK_NAMES, null, bookNamesValues);
                }

                // adds to the translations table
                translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_TRANSLATION_NAME, translationInfo.name);
                translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME,
                        translationInfo.shortName);

                if (translationInfo.shortName.equals("DA1871")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1843);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "Dansk");
                } else if (translationInfo.shortName.equals("KJV")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1817);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "English");
                } else if (translationInfo.shortName.equals("AKJV")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1799);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "English");
                } else if (translationInfo.shortName.equals("BBE")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1826);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "English");
                } else if (translationInfo.shortName.equals("ESV")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1780);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "English");
                } else if (translationInfo.shortName.equals("PR1938")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1950);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "Suomi");
                } else if (translationInfo.shortName.equals("FreSegond")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1972);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "Français");
                } else if (translationInfo.shortName.equals("Elb1905")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1990);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "Deutsche");
                } else if (translationInfo.shortName.equals("Lut1545")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1880);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "Deutsche");
                } else if (translationInfo.shortName.equals("Dio")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, "Italiano");
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, 1843);
                } else if (translationInfo.shortName.equals("개역성경")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1923);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "한국인");
                } else if (translationInfo.shortName.equals("PorAR")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1950);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "português");
                } else if (translationInfo.shortName.equals("RV1569")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1855);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "Español");
                } else if (translationInfo.shortName.equals("華語和合本")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1772);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "正體中文");
                } else if (translationInfo.shortName.equals("中文和合本")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1739);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "简体中文");
                } else if (translationInfo.shortName.equals("華語新譯本")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1874);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "正體中文");
                } else if (translationInfo.shortName.equals("中文新译本")) {
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, 1877);
                    translationInfoValues.put(TranslationsDatabaseHelper.COLUMN_LANGUAGE, "简体中文");
                }

                db.insert(TranslationsDatabaseHelper.TABLE_TRANSLATIONS, null, translationInfoValues);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public TranslationInfo[] translations()
    {
        SQLiteDatabase db = m_translationsDatabaseHelper.getReadableDatabase();
        String[] columns = new String[] { TranslationsDatabaseHelper.COLUMN_TRANSLATION_NAME,
                TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME,
                TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE, TranslationsDatabaseHelper.COLUMN_INSTALLED };
        Cursor cursor = db.query(TranslationsDatabaseHelper.TABLE_TRANSLATIONS, columns, null, null, null, null, null);
        if (cursor == null) {
            db.close();
            return null;
        }
        final int count = cursor.getCount();
        if (count == 0) {
            db.close();
            return null;
        }

//        final int languageColumnIndex = cursor.getColumnIndex(TranslationsDatabaseHelper.COLUMN_LANGUAGE);
        final int translationNameColumnIndex = cursor
                .getColumnIndex(TranslationsDatabaseHelper.COLUMN_TRANSLATION_NAME);
        final int translationShortNameColumnIndex = cursor
                .getColumnIndex(TranslationsDatabaseHelper.COLUMN_TRANSLATION_SHORTNAME);
        final int downloadSizeColumnIndex = cursor.getColumnIndex(TranslationsDatabaseHelper.COLUMN_DOWNLOAD_SIZE);
        final int installedColumnIndex = cursor.getColumnIndex(TranslationsDatabaseHelper.COLUMN_INSTALLED);
        TranslationInfo[] translations = new TranslationInfo[count];
        int i = 0;
        while (cursor.moveToNext()) {
            TranslationInfo translationinfo = new TranslationInfo();
            translationinfo.installed = (cursor.getInt(installedColumnIndex) == 1) ? true : false;
            translationinfo.size = cursor.getInt(downloadSizeColumnIndex);
            translationinfo.shortName = cursor.getString(translationShortNameColumnIndex);
            translationinfo.name = cursor.getString(translationNameColumnIndex);
//            translationinfo.language = cursor.getString(languageColumnIndex);
            translationinfo.path = translationinfo.shortName;
            translations[i++] = translationinfo;
        }
        db.close();
        return translations;
    }

    private TranslationsDatabaseHelper m_translationsDatabaseHelper;
}
