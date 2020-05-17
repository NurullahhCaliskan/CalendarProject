package com.example.calendarproject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DB extends SQLiteOpenHelper {
    private static final String EVENTTABLE = "create table " + DBYapisi.EVENT_TABLE_NAME + "(ID INTEGER PRIMARY KEY AUTOINCREMENT, "
            + DBYapisi.EVENT + " TEXT, " + DBYapisi.TIME + " TEXT, " + DBYapisi.DATE + " TEXT," + DBYapisi.MONTH + " TEXT, "
            + DBYapisi.YEAR + " TEXT," + DBYapisi.NOTIFY + " TEXT, " + DBYapisi.EVENTTYPE + " TEXT) ";

    private static final String DROP_EVENTS_TABLE = "DROP TABLE IF EXISTS " + DBYapisi.EVENT_TABLE_NAME;

    public DB(@Nullable Context context) {
        super(context, DBYapisi.DB_NAME, null, DBYapisi.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(EVENTTABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_EVENTS_TABLE);
        onCreate(db);

    }

    public void SaveEvent(String event, String time, String date, String month, String year, String notify, String eventtype, SQLiteDatabase database) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DBYapisi.EVENT, event);
        contentValues.put(DBYapisi.TIME, time);
        contentValues.put(DBYapisi.DATE, date);
        contentValues.put(DBYapisi.MONTH, month);
        contentValues.put(DBYapisi.YEAR, year);
        contentValues.put(DBYapisi.NOTIFY, notify);
        contentValues.put(DBYapisi.EVENTTYPE, eventtype);

        database.insert(DBYapisi.EVENT_TABLE_NAME, null, contentValues);
    }

    public Cursor ReadEvents(String date, SQLiteDatabase database) {
        String[] projections = {DBYapisi.EVENT, DBYapisi.TIME, DBYapisi.DATE, DBYapisi.MONTH, DBYapisi.YEAR};
        String selection = DBYapisi.DATE + "=?";
        String[] selectionArgs = {date};

        return database.query(DBYapisi.EVENT_TABLE_NAME, projections, selection, selectionArgs, null, null, null);
    }

    public Cursor ReadIDEvents(String date, String event, String time, SQLiteDatabase database) {
        String[] projections = {DBYapisi.ID, DBYapisi.NOTIFY, DBYapisi.EVENTTYPE};
        String selection = DBYapisi.DATE + "=? and " + DBYapisi.EVENT + " = ? and " + DBYapisi.TIME + " =?";
        String[] selectionArgs = {date, event, time};

        return database.query(DBYapisi.EVENT_TABLE_NAME, projections, selection, selectionArgs, null, null, null);
    }

    public Cursor ReadEventsPerMonth(String month, String year, SQLiteDatabase database) {
        //sql table not found hatası için bir kere yaptık
        //onUpgrade(database,1,1);
        String[] projections = {DBYapisi.EVENT, DBYapisi.TIME, DBYapisi.DATE, DBYapisi.MONTH, DBYapisi.YEAR};
        String selection = DBYapisi.MONTH + "=? and " + DBYapisi.YEAR + "=?";
        String[] selectionArgs = {month, year};

        return database.query(DBYapisi.EVENT_TABLE_NAME, projections, selection, selectionArgs, null, null, null);
    }

    public void DeleteEvent(String event, String date, String time, SQLiteDatabase database) {
        String selection = DBYapisi.EVENT + " = ? and " + DBYapisi.DATE + " = ? and " + DBYapisi.TIME + " = ?";
        String[] selectionArg = {event, date, time};
        database.delete(DBYapisi.EVENT_TABLE_NAME, selection, selectionArg);
    }

    public void UpdateEvent(String date, String event, String time, String notify, SQLiteDatabase database) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DBYapisi.NOTIFY, notify);
        String selection = DBYapisi.DATE + " = ? and " + DBYapisi.EVENT + " = ? and " + DBYapisi.TIME + " = ?";
        String[] selectionArg = {date, event, time};
        database.update(DBYapisi.EVENT_TABLE_NAME, contentValues, selection, selectionArg);


    }
}
