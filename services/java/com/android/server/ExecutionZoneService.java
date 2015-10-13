package com.android.server;

import android.content.ContentValues;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
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
import java.util.List;



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

    private static final String TABLE_POLICIES = "policies";
    private static final String POLICIES_ID = "_id";
    private static final String POLICIES_NAME = "policy_name";
    private static final String POLICIES_RULES = "policy_rules";

    private static final String TABLE_ZONEPOLICIES = "zonepolicies";
    private static final String ZONEPOLICIES_ID = "_id";
    private static final String ZONEPOLICIES_ZONE_ID = "zone_id";
    private static final String ZONEPOLICIES_POLICYLIST = "policy_list";

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

    private int getPolicyID (String policyName)
    {
        long zone_id_long = 0;

        synchronized (zonedbLock) {
            final SQLiteDatabase db = openHelper.getReadableDatabase();
            db.beginTransaction();
            try {
                zone_id_long = DatabaseUtils.longForQuery(db,
                        "select "+ POLICIES_ID +" from " + TABLE_POLICIES
                                + " WHERE " + POLICIES_NAME + "=?",
                        new String[]{policyName});

                db.setTransactionSuccessful();
            } catch (Exception e) {
                // Log, don't crash!
                Log.e(TAG, "Exception in getPolicyID");
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
        private static final int CREATE_ZONE = 100;
        private static final int SET_ZONE = 101;
        private static final int EDIT_ZONE = 102;
        private static final int CREATE_POLICY = 200;
        private static final int SET_POLICY = 201;
        private static final int EDIT_POLICY = 202;
        @Override
        public void handleMessage(Message msg) {
            try {
                if (msg.what == SET_ZONE) {
                    Log.i(TAG,"set zone message received:" + msg.getData().getString("packagename") + " assign to zone " + msg.getData().getString("zonename"));
                    if(setZoneToApp(msg.getData().getString("packagename"),msg.getData().getString("zonename")))
                        Log.i(TAG, "zone info inserted successfully.");
                }
                if (msg.what == CREATE_ZONE) {
                    Log.i(TAG,"create zone message received:" + msg.getData().getString("packagename") + " assign to zone " + msg.getData().getString("zonename"));

                }
                if (msg.what == EDIT_ZONE) {
                    Log.i(TAG,"create zone message received:" + msg.getData().getString("packagename") + " assign to zone " + msg.getData().getString("zonename"));

                }
                if (msg.what == CREATE_POLICY) {
                    Log.i(TAG,"create zone message received:" + msg.getData().getString("packagename") + " assign to zone " + msg.getData().getString("zonename"));

                }
                if (msg.what == EDIT_POLICY) {
                    Log.i(TAG,"create zone message received:" + msg.getData().getString("packagename") + " assign to zone " + msg.getData().getString("zonename"));

                }
                if (msg.what == SET_POLICY) {
                    Log.i(TAG,"create zone message received:" + msg.getData().getString("packagename") + " assign to zone " + msg.getData().getString("zonename"));

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

    private static String getPolicyString(String policyName) {

        String policyString = "";

        if(policyName.equals("NEW"))
            policyString = getDefaultAllDangerousDenyPolicyString();
        else if(policyName.equals("TRUSTED"))
            policyString = getDefaultAllDangerousDenyPolicyString();
        else if(policyName.equals("UNTRUSTED"))
            policyString = getDefaultAllDangerousDenyPolicyString();
        else if(policyName.equals("RESTRICTED"))
            policyString = getDefaultAllDangerousDenyPolicyString();
        else
        {
            //go to database and return
        }

        return policyString;
    }

    private static String getDefaultAllDangerousDenyPolicyString() {

        return "<DEFAULT,READ_CALENDAR,TIME_ALWAYS,DENY>;" +
                "<DEFAULT,WRITE_CALENDAR,TIME_ALWAYS,DENY>;" +
                "<DEFAULT,CAMERA,TIME_ALWAYS,DENY>;" +
                "<DEFAULT,READ_CONTACTS,TIME_ALWAYS,DENY>;" +
                "<DEFAULT,WRITE_CONTACTS,TIME_ALWAYS,DENY>;" +
                "<DEFAULT,GET_ACCOUNTS,TIME_ALWAYS,DENY>;" +
                "<DEFAULT,ACCESS_FINE_LOCATION,TIME_ALWAYS,DENY>;" +
                "<DEFAULT,ACCESS_COARSE_LOCATION,TIME_ALWAYS,DENY>;" +
                "<DEFAULT,RECORD_AUDIO,TIME_ALWAYS,DENY>;" +
                "<DEFAULT,READ_PHONE_STATE,TIME_ALWAYS,DENY>;" +
                "<DEFAULT,CALL_PHONE,TIME_ALWAYS,DENY>;" +
                "<DEFAULT,READ_CALL_LOG,TIME_ALWAYS,DENY>;" +
                "<DEFAULT,WRITE_CALL_LOG,TIME_ALWAYS,DENY>;" +
                "<DEFAULT,ADD_VOICEMAIL,TIME_ALWAYS,DENY>;" +
                "<DEFAULT,USE_SIP,TIME_ALWAYS,DENY>;" +
                "<DEFAULT,PROCESS_OUTGOING_CALLS,TIME_ALWAYS,DENY>;" +
                "<DEFAULT,BODY_SENSORS,TIME_ALWAYS,DENY>;" +
                "<DEFAULT,SEND_SMS,TIME_ALWAYS,DENY>;" +
                "<DEFAULT,RECEIVE_SMS,TIME_ALWAYS,DENY>;" +
                "<DEFAULT,READ_SMS,TIME_ALWAYS,DENY>;" +
                "<DEFAULT,RECEIVE_WAP_PUSH,TIME_ALWAYS,DENY>;" +
                "<DEFAULT,RECEIVE_MMS,TIME_ALWAYS,DENY>;" +
                "<DEFAULT,READ_EXTERNAL_STORAGE,TIME_ALWAYS,DENY>;" +
                "<DEFAULT,WRITE_EXTERNAL_STORAGE,TIME_ALWAYS,DENY>";
    }

    static class DatabaseHelper extends SQLiteOpenHelper {

        Context dbhContext;

        public DatabaseHelper(Context context) {
            super(context, ExecutionZoneService.getDatabaseName(), null, DATABASE_VERSION);

            dbhContext = context;
        }

        /**
         * This call needs to be made while the mCacheLock is held. The way to
         * ensure this is to get the lock any time a method is called ont the DatabaseHelper
         * @param db The database.
         */
        @Override
        public void onCreate(SQLiteDatabase db) {

            try {
                //create table for zones
                db.execSQL("CREATE TABLE " + TABLE_ZONES + " ( "
                        + ZONES_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + ZONES_NAME + " TEXT NOT NULL, "
                        + "UNIQUE(" + ZONES_NAME + "))");

                //add default zones shah shah

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

                //create table to save zone to app mapping

                db.execSQL("CREATE TABLE " + TABLE_APPZONES + " (  "
                        + APPZONES_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,  "
                        + APPZONES_ZONE_ID + " INTEGER NOT NULL, "
                        + APPZONES_APP_NAME + " TEXT NOT NULL,  "
                        + "UNIQUE (" + APPZONES_APP_NAME + "))");

                //ADD FOR LOOP TO ADD ALL SYSTEM APPS TO TRUSTED ZONE SHAH SHAH OCT 6

                PackageManager packageManager = dbhContext.getPackageManager();
                List<ApplicationInfo> list = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

                int zoneID = 0;

                Cursor c = db.rawQuery("select " + ZONES_ID + " from " + TABLE_ZONES
                                + " WHERE " + ZONES_NAME + "=?",
                        new String[]{"TRUSTED"});

                if (c.getCount() > 0) {
                    c.moveToFirst();

                    zoneID = c.getInt(0);
                }

                for (ApplicationInfo info : list) {

                    db.execSQL("INSERT OR REPLACE INTO " + TABLE_APPZONES + " (  "
                            + APPZONES_ZONE_ID + ", "
                            + APPZONES_APP_NAME + ") "
                            + " VALUES ( "
                            + zoneID
                            + ", '" + info.packageName + "'"
                            + ");");
                }

                //create table for policy
                db.execSQL("CREATE TABLE " + TABLE_POLICIES + " (  "
                        + POLICIES_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,  "
                        + POLICIES_NAME + " TEXT NOT NULL, "
                        + POLICIES_RULES + " TEXT NOT NULL,  "
                        + "UNIQUE (" + POLICIES_NAME + "))");

                //add default policies
                db.execSQL("INSERT OR REPLACE INTO " + TABLE_POLICIES + " (  "
                        + POLICIES_NAME + ", "
                        + POLICIES_RULES + ") "
                        + " VALUES ( "
                        + "'DENY_ALL_DANGEROUS'"
                        + ", '" + getDefaultAllDangerousDenyPolicyString() + "'"
                        + ");");

                //create table for zone to policy mapping
                db.execSQL("CREATE TABLE " + TABLE_ZONEPOLICIES + " (  "
                        + ZONEPOLICIES_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,  "
                        + ZONEPOLICIES_ZONE_ID + " INTEGER NOT NULL, "
                        + ZONEPOLICIES_POLICYLIST + " TEXT NOT NULL,  "
                        + "UNIQUE (" + APPZONES_APP_NAME + "))");

                //add default zone policy mappings

                c = db.rawQuery("select " + ZONES_ID + " from " + TABLE_ZONES
                                + " WHERE " + ZONES_NAME + "=?",
                        new String[]{"NEW"});

                if (c.getCount() > 0) {
                    c.moveToFirst();

                    zoneID = c.getInt(0);
                }

                int policyID = 0;

                c = db.rawQuery("select " + POLICIES_ID + " from " + TABLE_POLICIES
                                + " WHERE " + POLICIES_NAME + "=?",
                        new String[]{"DENY_ALL_DANGEROUS"});

                if (c.getCount() > 0) {
                    c.moveToFirst();

                    policyID = c.getInt(0);
                }

                db.execSQL("INSERT OR REPLACE INTO " + TABLE_ZONEPOLICIES + " (  "
                        + ZONEPOLICIES_ZONE_ID + ", "
                        + ZONEPOLICIES_POLICYLIST + ") "
                        + " VALUES ( "
                        + zoneID
                        + ", '" + policyID + "'"
                        + ");");

                c = db.rawQuery("select " + ZONES_ID + " from " + TABLE_ZONES
                                + " WHERE " + ZONES_NAME + "=?",
                        new String[]{"UNTRUSTED"});

                if (c.getCount() > 0) {
                    c.moveToFirst();

                    zoneID = c.getInt(0);
                }

                db.execSQL("INSERT OR REPLACE INTO " + TABLE_ZONEPOLICIES + " (  "
                        + ZONEPOLICIES_ZONE_ID + ", "
                        + ZONEPOLICIES_POLICYLIST + ") "
                        + " VALUES ( "
                        + zoneID
                        + ", '" + policyID + "'"
                        + ");");

                c = db.rawQuery("select " + ZONES_ID + " from " + TABLE_ZONES
                                + " WHERE " + ZONES_NAME + "=?",
                        new String[]{"RESTRICTED"});

                if (c.getCount() > 0) {
                    c.moveToFirst();

                    zoneID = c.getInt(0);
                }
                db.execSQL("INSERT OR REPLACE INTO " + TABLE_ZONEPOLICIES + " (  "
                        + ZONEPOLICIES_ZONE_ID + ", "
                        + ZONEPOLICIES_POLICYLIST + ") "
                        + " VALUES ( "
                        + zoneID
                        + ", '" + policyID + "'"
                        + ");");
            }
            catch (Exception once)
            {
                Log.e(TAG, "Exception occurred while creating zones.db. The exception message is; "+ once.getMessage());
            }
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
