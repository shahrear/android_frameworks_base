package com.android.server;

import android.content.ContentValues;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.os.Handler;
import android.os.IExecutionZoneService;
import android.os.Looper;
import android.os.Bundle;
import android.content.Context;
import android.os.Message;
import android.util.Log;

import java.io.File;


/**
 * Created by shahrear on 10/5/15.
 */
public class ExecutionZoneService extends IExecutionZoneService.Stub {
    private static final String TAG = "ExecutionZoneService";

    private static final String DATABASE_NAME = "zones.db";
    private static final int DATABASE_VERSION = 5;

    private static final String TABLE_ZONES = "zones";
    private static final String ZONES_ID = "_id";
    private static final String ZONES_NAME = "zone_name";

    private static final String TABLE_APPZONES = "appzones";
    private static final String APPZONES_ID = "_id";
    private static final String APPZONES_APP_NAME = "app_name";
    private static final String APPZONES_ZONE_ID = "zone_id";

    private final Object zonedbLock = new Object();
    private final DatabaseHelper openHelper;

    private ExecutionZoneWorkerThread mWorker;
    private ExecutionZoneWorkerHandler mHandler;
    private Context mContext;
    public ExecutionZoneService(Context context) {
        super();

        synchronized (zonedbLock) {
            openHelper = new DatabaseHelper(context);
        }

        mContext = context;
        mWorker = new ExecutionZoneWorkerThread("ExecutionZoneServiceWorker");
        mWorker.start();
        Log.i(TAG, "Spawned worker thread");
    }

    public void setZone(String packageName, String zoneName) {
        Log.i(TAG, "setZone " + packageName + "to zone " + zoneName);

        // Creating Bundle object
        Bundle b = new Bundle();

        // Storing data into bundle
        b.putString("what", "SET_ZONE");
        b.putString("packagename", packageName);
        b.putString("zonename", zoneName);

        Message msg = Message.obtain();
        msg.what = ExecutionZoneWorkerHandler.SET_ZONE;
        msg.setData(b);
        mHandler.sendMessage(msg);
    }

    private class ExecutionZoneWorkerThread extends Thread {
        public ExecutionZoneWorkerThread(String name) {
            super(name);
        }
        public void run() {
            Looper.prepare();
            mHandler = new ExecutionZoneWorkerHandler();
            Looper.loop();
        }
    }

    private int getZoneID (String zoneName)
    {
        long zone_id_long = 0;

        synchronized (zonedbLock) {
            final SQLiteDatabase db = openHelper.getReadableDatabase();
            db.beginTransaction();
            try {
                zone_id_long = DatabaseUtils.longForQuery(db,
                        "select "+ ZONES_ID +" from " + TABLE_ZONES
                                + " WHERE " + ZONES_NAME + "=?",
                        new String[]{zoneName});

                db.setTransactionSuccessful();
            } catch (Exception e) {
                // Log, don't crash!
                Log.e(TAG, "Exception in getZoneID");
            } finally {
                db.endTransaction();
            }
        }

        return (int) zone_id_long;
    }

    private boolean setZoneToApp (String packageName, String zoneName)
    {
        synchronized (zonedbLock) {
            final SQLiteDatabase db = openHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                int zone_id = getZoneID(zoneName);
                ContentValues values = new ContentValues();
                values.put(APPZONES_APP_NAME, packageName);
                values.put(APPZONES_ZONE_ID, zone_id);

                long appzoneId = db.insert(TABLE_APPZONES, APPZONES_APP_NAME, values);
                if (appzoneId < 0) {
                    Log.w(TAG, "insertZoneIntoDatabase: " + zone_id
                            + ", skipping the DB insert failed");
                    return false;
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

        }

        return true;
    }

    private class ExecutionZoneWorkerHandler extends Handler {
        private static final int SET_ZONE = 0;
        @Override
        public void handleMessage(Message msg) {
            try {
                if (msg.what == SET_ZONE) {
                    Log.i(TAG,"set zone message received:" + msg.getData().getString("packagename") + "assign to zone " + msg.getData().getString("zonename"));

                }
            } catch (Exception e) {
                // Log, don't crash!
                Log.e(TAG, "Exception in handleMessage");
            }
        }
    }


    private static String getDatabaseName() {
        File systemDir = Environment.getSystemSecureDirectory();
        File databaseFile = new File(systemDir, DATABASE_NAME);

        return databaseFile.getPath();
    }

    static class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(Context context) {
            super(context, ExecutionZoneService.getDatabaseName(), null, DATABASE_VERSION);
        }

        /**
         * This call needs to be made while the mCacheLock is held. The way to
         * ensure this is to get the lock any time a method is called ont the DatabaseHelper
         * @param db The database.
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_ZONES + " ( "
                    + ZONES_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + ZONES_NAME + " TEXT NOT NULL, "
                    + "UNIQUE(" + ZONES_NAME + "))");

            db.execSQL("INSERT OR REPLACE INTO " + TABLE_ZONES + " ( "
                    + ZONES_NAME + ") "
                    + " VALUES ( "
                    + "'NEW' );");

            db.execSQL("INSERT OR REPLACE INTO " + TABLE_ZONES + " ( "
                    + ZONES_NAME + ") "
                    + " VALUES ( "
                    + "'TRUSTED' );");

            db.execSQL("INSERT OR REPLACE INTO " + TABLE_ZONES + " ( "
                    + ZONES_NAME + ") "
                    + " VALUES ( "
                    + "'UNTRUSTED' );");

            db.execSQL("INSERT OR REPLACE INTO " + TABLE_ZONES + " ( "
                    + ZONES_NAME + ") "
                    + " VALUES ( "
                    + "'RESTRICTED' );");

            db.execSQL("CREATE TABLE " + TABLE_APPZONES + " (  "
                    + APPZONES_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,  "
                    + APPZONES_ZONE_ID + " INTEGER NOT NULL, "
                    + APPZONES_APP_NAME + " TEXT NOT NULL,  "
                    + "UNIQUE (" + APPZONES_APP_NAME + "))");


            //ADD FOR LOOP TO ADD ALL SYSTEM APPS TO TRUSTED ZONE SHAH SHAH OCT 6
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.e(TAG, "upgrade from version " + oldVersion + " to version " + newVersion);

            if (oldVersion < newVersion) {
                // no longer need to do anything since the work is done
                // when upgrading from version 2
                oldVersion = newVersion;
            }


            if (oldVersion != newVersion) {
                Log.e(TAG, "failed to upgrade version " + oldVersion + " to version " + newVersion);
            }
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "opened database " + DATABASE_NAME);
        }
    }



}
