package com.voice.chat.utils;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class WholeGameDataBase extends SQLiteOpenHelper {

    private final static String TAG = "voice";

    private static final int VERSION = 1;
    private static final String DATABASE_NAME = "whole_game";
    private static final String GAME_LIST_TABLE_NAME = "whole_game_list";
    private static final String PACKAGE_NAME = "package_name";
    private static final String STATUS = "status";
    private static final String GAME_LIST_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS "
            + GAME_LIST_TABLE_NAME
            + " ("
            + PACKAGE_NAME
            + " VARCHAR PRIMARY KEY NOT NULL UNIQUE , "
            + STATUS
            + " INTEGER NOT NULL DEFAULT 0)";

    public WholeGameDataBase(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(GAME_LIST_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public boolean insertPackageName(String name) {
        boolean succ = false;
        SQLiteDatabase db = null;
        if (name != null) {
            try {
                db = getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put(PACKAGE_NAME, name);
                values.put(STATUS, 1);

                long rowId = -1;
                rowId = db.insert(GAME_LIST_TABLE_NAME, "", values);
                if (rowId > 0) {
                    succ = true;
                }
            } catch (Exception e) {
                Log.e(TAG, "insertPackageName error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (db != null) {
                    db.close();
                }
            }
        }

        return succ;
    }

    public boolean addPackageNameList(ArrayList<String> nameArray) {
        boolean succ = false;
        for (String name : nameArray) {
            succ |= insertPackageName(name);
        }

        return succ;
    }

    public boolean isGamePackage(String packageName) {
        boolean is = false;
        SQLiteDatabase db = null;
        Cursor cursor = null;

        if (packageName != null) {
            try {
                db = getReadableDatabase();
                String selection = PACKAGE_NAME + "=?";
                String selectionArgs[] = new String[] { packageName };
                cursor = db.query(GAME_LIST_TABLE_NAME, null, selection,
                        selectionArgs, null, null, null);

                if (cursor.getCount() > 0) {
                    is = true;
                }
            } catch (Exception e) {
                Log.e(TAG, "isGamePackage error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }

                if (db != null) {
                    db.close();
                }
            }
        }
        return is;
    }
}
