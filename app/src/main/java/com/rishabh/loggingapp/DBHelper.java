package com.rishabh.loggingapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Rishabh on 4/20/2018.
 */

public class DBHelper extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "Logs";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE " + "Accelerometer" + "("
                + "id" + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "x_val" + "REAL,"
                + "y_val" + "REAL,"
                + "z_val" + "REAL,"
                + "timestamp" + " DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ")");
        sqLiteDatabase.execSQL("CREATE TABLE " + "Gyroscope" + "("
                + "id" + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "x_val" + "REAL,"
                + "y_val" + "REAL,"
                + "z_val" + "REAL,"
                + "timestamp" + " DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ")");
        sqLiteDatabase.execSQL("CREATE TABLE " + "GPS" + "("
                + "id" + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "latitude" + "REAL,"
                + "longitude" + "REAL,"
                + "timestamp" + " DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ")");
        sqLiteDatabase.execSQL("CREATE TABLE " + "WiFi" + "("
                + "id" + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "ssid" + "TEXT,"
                + "timestamp" + " DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ")");
        sqLiteDatabase.execSQL("CREATE TABLE " + "Mic" + "("
                + "id" + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "decibel" + "REAL,"
                + "timestamp" + " DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ")");
        sqLiteDatabase.execSQL("CREATE TABLE " + "Network" + "("
                + "id" + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "cid" + "INTEGER,"
                + "timestamp" + " DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + "Accelerometer");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + "Gyroscope");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + "GPS");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + "WiFi");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + "Mic");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + "Network");
        onCreate(sqLiteDatabase);
    }

    public long insertAccelerometerData(Float x_val, Float y_val, Float z_val) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("x_val", x_val);
        values.put("y_val", y_val);
        values.put("z_val", z_val);
        long id = db.insert("Accelerometer", null, values);
        db.close();
        return  id;
    }

    public long insertGyroscopeData(Float x_val, Float y_val, Float z_val) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("x_val", x_val);
        values.put("y_val", y_val);
        values.put("z_val", z_val);
        long id = db.insert("Gyroscope", null, values);
        db.close();
        return  id;
    }

    public long insertGPSData(Double latitude, Double longitude) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("latitude", latitude);
        values.put("longitude", longitude);
        long id = db.insert("GPS", null, values);
        db.close();
        return  id;
    }

    public long insertWiFiData(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        long id = db.insert("WiFi", null, values);
        db.close();
        return  id;
    }

    public long insertMicData(Double decibel) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("decibel", decibel);
        long id = db.insert("Mic", null, values);
        db.close();
        return  id;
    }

    public long insertNetworkData(int cid) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("cid", cid);
        long id = db.insert("Network", null, values);
        db.close();
        return  id;
    }
}
