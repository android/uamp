package com.example.chip7.musicstore;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by chip7 on 14/05/2017.
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    //Create Database & tables.
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "store.db";
    private static final String TABLE_NAME = "users";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_EMAIL = "email";
    private static final String COLUMN_UNAME = "uname";
    private static final String COLUMN_PASS = "pass";
    SQLiteDatabase db;

    private static final String TABLE_CREATE = "create table users (id integer primary key not null , " +
            "name text not null, email text not null, uname text not null, pass text not null)";

    //Constructor
    public DatabaseHelper(Context context) {
        super(context , DATABASE_NAME, null, DATABASE_VERSION);
    }



    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
        this.db = db;
    }

    //Inserting User data into DB, via grouping variables.
    public void insertContact(Contact c) {
        db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        //Creating Unique ID.
        String query = " select * from users ";
        Cursor cursor = db.rawQuery(query, null);
        int count = cursor.getCount();

        values.put(COLUMN_ID, count);
        values.put(COLUMN_NAME, c.getName());
        values.put(COLUMN_EMAIL, c.getEmail());
        values.put(COLUMN_UNAME, c.getUname());
        values.put(COLUMN_PASS, c.getPass());

        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    //Reading from the the Database
    public String searchPass(String pass){
        db = this.getReadableDatabase();
        String query = " select * from " + TABLE_NAME;
        Cursor cursor = db.rawQuery(query, null);
        String a, b;
        b = "Not Found";

        if(cursor.moveToFirst()){
            do{
                a = cursor.getString(0);

                if(a.equals(pass)){
                    b = cursor.getString(1);
                    break;
                }
            }
            while(cursor.moveToNext());
        }

        return b;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String query = " DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(query);
        this.onCreate(db);
    }
}
