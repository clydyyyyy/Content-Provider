package com.gege.todo.Utils;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ContentHandler extends ContentProvider {

    static final String PROVIDER_NAME = "com.gege.todo.list";
    public static final Uri CONTENT_URI = Uri.parse("content://"+ PROVIDER_NAME + "/list");
    private SQLiteDatabase db;
    private static final String TODO_TABLE = "todo";
    private static final String ID = "id";
    private static final String TASK = "task";
    static final int NOTES = 1;
    static final int NOTE_ID = 2;
    private static UriMatcher uriMatcher = null;
    static{
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(PROVIDER_NAME, "books", NOTES);
        uriMatcher.addURI(PROVIDER_NAME, "books/#", NOTE_ID);
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();
        DatabaseHandler dbHelper = new DatabaseHandler(context);
        db = dbHelper.getWritableDatabase();
        return (db == null)? false:true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
        sqlBuilder.setTables(TODO_TABLE);
        if (uriMatcher.match(uri) == NOTE_ID)
//---if getting a particular book---
            sqlBuilder.appendWhere(
                    ID + " = " + uri.getPathSegments().get(1));
        if (sortOrder==null || sortOrder=="")
            sortOrder = TASK;
        Cursor c = sqlBuilder.query(
                db,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)){
//---get all  notes---
            case NOTES:
                return "vnd.android.cursor.dir/vnd.appnotes.notes ";
//---get a particular note---
            case NOTE_ID:
                return "vnd.android.cursor.item/vnd.appnotes.notes ";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
//---add a new book---
        long rowID = db.insert(TODO_TABLE, "", contentValues);
//---if added successfully---
        if (rowID>0)
        {
            Uri _uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(_uri, null);
            return _uri;
        }
        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
// arg0 = uri
// arg1 = selection
// arg2 = selectionArgs
        int count=0;
        switch (uriMatcher.match(uri)){
            case NOTES:
                count = db.delete(
                        TODO_TABLE,s, strings);
                break;
            case NOTE_ID:
                String id = uri.getPathSegments().get(1);count = db.delete(
                    TODO_TABLE, ID + " = " + id + (!TextUtils.isEmpty(s) ? " AND (" +s + ')' : ""), strings);
                break;
            default: throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int count = 0;
        switch (uriMatcher.match(uri)){
            case NOTES:
                count = db.update(
                        TODO_TABLE,
                        values,
                        selection,
                        selectionArgs);
                break;
            case NOTE_ID:
                count = db.update(
                        TODO_TABLE,
                        values,
                        ID + " = " + uri.getPathSegments().get(1) +
                                (!TextUtils.isEmpty(selection) ? " AND (" +
                                        selection + ')' : ""),
                        selectionArgs);
                break;
            default: throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
