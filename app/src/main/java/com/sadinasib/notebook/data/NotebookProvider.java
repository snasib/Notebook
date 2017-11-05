package com.sadinasib.notebook.data;

import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import static com.sadinasib.notebook.data.NotebookContract.CONTENT_AUTHORITY;
import static com.sadinasib.notebook.data.NotebookContract.NotebookEntry;
import static com.sadinasib.notebook.data.NotebookContract.PATH_WORD;

/**
 * Created by sadin on 05-Nov-17.
 */

public class NotebookProvider extends ContentProvider {
    private static final String TAG = NotebookProvider.class.getSimpleName();
    private static final int WORDS = 100;
    private static final int WORD_ID = 101;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_WORD, WORDS);
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_WORD + "/#", WORDS);
    }

    private NotebookDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        Log.i(TAG, "onCreate");
        mDbHelper = new NotebookDbHelper(getContext());
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        Log.i(TAG, "query: " + uri.toString());
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        Cursor cursor;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case WORDS:
                cursor = db.query(
                        NotebookEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case WORD_ID:
                selection = NotebookEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = db.query(
                        NotebookEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
            default:
                throw new IllegalArgumentException("Cannot query unknown URI" + uri);
        }
        //noinspection ConstantConditions
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        Log.i(TAG, "getType");
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case WORDS:
                return NotebookEntry.CONTENT_LIST_TYPE;
            case WORD_ID:
                return NotebookEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        Log.i(TAG, "insert: " + uri.toString());
        int match = sUriMatcher.match(uri);
        switch (match) {
            case WORDS:
                return insertWord(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    @Nullable
    private Uri insertWord(Uri uri, ContentValues contentValues) {
        Log.i(TAG, "insertWord: " + uri.toString());

        if (contentValues != null) {
            String word = contentValues.getAsString(NotebookEntry.COLUMN_WORD);
            if (TextUtils.isEmpty(word)) {
                throw new IllegalArgumentException("Dictionary requires a word");
            }
            String translation = contentValues.getAsString(NotebookEntry.COLUMN_TRANSLATION);
            if (TextUtils.isEmpty(translation)) {
                throw new IllegalArgumentException("Dictionary requires a translation");
            }
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long id = db.insert(NotebookEntry.TABLE_NAME, null, contentValues);
        if (id == -1) {
            Log.e(TAG, "insertWord: Failed to insert " + uri.toString());
            return null;
        }
        //noinspection ConstantConditions
        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        Log.i(TAG, "delete: " + uri.toString());
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case WORDS:
                int id = db.delete(NotebookEntry.TABLE_NAME, selection, selectionArgs);
                //noinspection ConstantConditions
                getContext().getContentResolver().notifyChange(uri, null);
                return id;
            case WORD_ID:
                selection = NotebookEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                id = db.delete(NotebookEntry.TABLE_NAME, selection, selectionArgs);
                //noinspection ConstantConditions
                getContext().getContentResolver().notifyChange(uri, null);
                return id;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri.toString());
        }
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String selection, @Nullable String[] selectionArgs) {
        Log.i(TAG, "update: " + uri.toString());
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case WORDS:
                return updateWord(uri, contentValues, selection, selectionArgs);
            case WORD_ID:
                selection = NotebookEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updateWord(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    private int updateWord(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        Log.i(TAG, "updateWord: " + uri.toString());
        if (contentValues != null) {
            String word = contentValues.getAsString(NotebookEntry.COLUMN_WORD);
            if (TextUtils.isEmpty(word)) {
                throw new IllegalArgumentException("Dictionary requires a word");
            }
            String translation = contentValues.getAsString(NotebookEntry.COLUMN_TRANSLATION);
            if (TextUtils.isEmpty(translation)) {
                throw new IllegalArgumentException("Dictionary requires a translation");
            }
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int updateId = db.update(NotebookEntry.TABLE_NAME, contentValues, selection, selectionArgs);
        //noinspection ConstantConditions
        getContext().getContentResolver().notifyChange(uri, null);
        return updateId;
    }
}
