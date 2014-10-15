package com.voice.chat.utils;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class PhoneGameDataBase extends SQLiteOpenHelper {

    private final static String TAG = "voice";

    private static final int VERSION = 1;
    private static final String DATABASE_NAME = "phone_game";
    private static final String GAME_LIST_TABLE_NAME = "game_in_phone";
    private static final String PACKAGE_NAME = "package_name";
    private static final String MAIN_ACTIVITY_NAME = "main_activity";
    private static final String STATUS = "status";
    private static final String GAME_LIST_TABLE_CREATE = "CREATE TABLE IF NOT EXISTS "
            + GAME_LIST_TABLE_NAME
            + " ("
            + PACKAGE_NAME
            + " VARCHAR PRIMARY KEY NOT NULL UNIQUE , "
            + STATUS
            + " INTEGER NOT NULL DEFAULT 0)";

    public PhoneGameDataBase(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(GAME_LIST_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public boolean isGame(String packageName) {
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
                Log.e(TAG, "isGame error: " + e.getMessage());
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

    public ArrayList<String> getPhoneGameList() {
        ArrayList<String> gameList = new ArrayList<String>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            String name = null;
            db = getReadableDatabase();
            cursor = db.query(GAME_LIST_TABLE_NAME, null, null, null, null,
                    null, null);

            if (cursor.getCount() > 0) {
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor
                        .moveToNext()) {
                    name = cursor.getString(0);
                    gameList.add(name);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getPhoneGameList error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }

            if (db != null) {
                db.close();
            }
        }

        return gameList;
    }

    public void updatePhoneGameList() {

    }

    public void cleanPhoneGameList() {
        SQLiteDatabase db = getWritableDatabase();

        if (db != null) {
            try {
                db.delete(GAME_LIST_TABLE_NAME, null, null);
            } catch (Exception e) {
                Log.d(TAG,
                        "cleanPhoneGameList delete exception:" + e.getMessage());
            } finally {
                db.close();
            }
        } else {
            Log.d(TAG, "cleanPhoneGameList error: db is null"
                    + GAME_LIST_TABLE_NAME);
        }
    }

    public boolean insertPhoneGame(String name) {
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
            } catch (SQLiteConstraintException e) {
                Log.w(TAG, "insertPackageName warning: " + e.getMessage());
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
}
