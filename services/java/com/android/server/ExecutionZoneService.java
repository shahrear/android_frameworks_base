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

import com.google.android.collect.Lists;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Created by shahrear on 10/5/15.
 */
public class ExecutionZoneService extends IExecutionZoneService.Stub {
    private static final String TAG = "ExecutionZoneService";

    private static final String DATABASE_NAME = "zones.db";
    private static final int DATABASE_VERSION = 5;

    private static final boolean DEBUG_ENABLE = true;

    private static final int PERMISSION_PERMITTED_IN_ZONE = 100;
    private static final int PERMISSION_NOT_PERMITTED_IN_ZONE = -100;

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
        Log.i(TAG, "Log SHAH Spawned worker thread");
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

    private class ExecutionZoneWorkerHandler extends Handler {
        private static final int CREATE_ZONE = 100;
        private static final int SET_ZONE = 101;
        private static final int EDIT_ZONE = 102;
        private static final int CREATE_POLICY = 200;
        private static final int SET_POLICY = 201;
        private static final int EDIT_POLICY = 202;

        private static final int CHECK_ZONE_PERMISSION = 300;
        @Override
        public void handleMessage(Message msg) {
            try {
                if (msg.what == SET_ZONE) {
                    Log.i(TAG,"set zone message received:" + msg.getData().getString("packagename") + " assign to zone " + msg.getData().getString("zonename"));
                    if(setZoneToApp(msg.getData().getString("packagename"),msg.getData().getString("zonename")))
                        Log.i(TAG, "Log SHAH zone info inserted successfully.");
                }
                if (msg.what == CREATE_ZONE) {
                    Log.i(TAG,"create zone message received: zonename: " + msg.getData().getString("zonename") + " policylist " + msg.getData().getString("policylist"));
                    if(createZoneWithPolicies(msg.getData().getString("zonename"),msg.getData().getString("policylist")))
                        Log.i(TAG, "Log SHAH zone created successfully.");
                }
                if (msg.what == EDIT_ZONE) {
                    Log.i(TAG,"edit zone message received: zone: " + msg.getData().getString("zonename") + " action " + msg.getData().getString("action") + " param: "+msg.getData().getString("paramlist"));
                    if(editZoneOrPolicies(msg.getData().getString("zonename"), msg.getData().getString("action"),msg.getData().getString("paramlist")))
                        Log.i(TAG, "Log SHAH zone edited successfully.");

                }
                if (msg.what == CREATE_POLICY) {
                    Log.i(TAG,"create policy message received: policyname " + msg.getData().getString("policyname") + " rule list " + msg.getData().getString("rulelist"));
                    if(createPolicyInDB(msg.getData().getString("policyname"), msg.getData().getString("rulelist")))
                        Log.i(TAG, "Log SHAH policy created successfully.");

                }
                if (msg.what == EDIT_POLICY) {
                    Log.i(TAG,"edit policy message received: policy: " + msg.getData().getString("policyname") + " action " + msg.getData().getString("action") + " param: "+msg.getData().getString("paramlist"));
                    if(editPoliciesInDB(msg.getData().getString("policyname"), msg.getData().getString("action"), msg.getData().getString("paramlist")))
                        Log.i(TAG, "Log SHAH policy edited successfully.");

                }
                if (msg.what == SET_POLICY) {
                    Log.i(TAG,"set policy message received: policyname: " + msg.getData().getString("policyname") + " zone " + msg.getData().getString("zonename"));
                    if(setPolicyToZone(msg.getData().getString("policyname"), msg.getData().getString("zonename")))
                        Log.i(TAG, "Log SHAH set policy to zone successfully.");
                }
            } catch (Exception e) {
                // Log, don't crash!
                Log.e(TAG, "Log SHAH Exception in handleMessage");
            }
        }
    }


    public void setZone(String packageName, String zoneName) {
        Log.i(TAG, "Log SHAH setZone " + packageName + "to zone " + zoneName);

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

    //if not present, insert, otherwise update
    private boolean setZoneToApp (String packageName, String zoneName)
    {
        synchronized (zonedbLock) {
            final SQLiteDatabase db = openHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                int zone_id = getZoneID(zoneName);

                Log.d(TAG,"Set zone: "+zoneName+" to package: "+packageName);
                long isPackageInstalled = 0;
                try {
                    isPackageInstalled = DatabaseUtils.queryNumEntries(db, TABLE_APPZONES, APPZONES_APP_NAME + "=?", new String[]{packageName});
                    Log.d(TAG,"ispackageinstalled value in setzone: "+isPackageInstalled);
                }
                catch (Exception e)
                {
                    Log.e(TAG,"Error in queryNumEntries in setzone");
                    return false;
                }

                ContentValues values = new ContentValues();

                if(isPackageInstalled>0)
                {
                    values.put(APPZONES_ZONE_ID, zone_id);

                    long appzoneId = db.update(TABLE_APPZONES, values, APPZONES_APP_NAME + "='" +packageName+"'", null);

                    if (appzoneId < 0) {
                        Log.w(TAG, "Log SHAH update Zone Into Database: " + zone_id + " for packagename: " + packageName
                                + ", skipping the DB update failed");
                        return false;
                    }

                }
                else
                {
                    values.put(APPZONES_APP_NAME, packageName);
                    values.put(APPZONES_ZONE_ID, zone_id);

                    long appzoneId = db.insert(TABLE_APPZONES, null, values);

                    if (appzoneId < 0) {
                        Log.w(TAG, "Log SHAH insertZoneIntoDatabase: " + zone_id + " for package: " + packageName
                                + ", skipping the DB insert failed");
                        return false;
                    }
                }



                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

        }

        return true;
    }
    public void createZone(String zoneName, String policyList) {
        Log.i(TAG, "Log SHAH create Zone " + zoneName + "with policies " + policyList);

        // Creating Bundle object
        Bundle b = new Bundle();

        // Storing data into bundle
        b.putString("zonename", zoneName);
        b.putString("policylist", policyList);

        Message msg = Message.obtain();
        msg.what = ExecutionZoneWorkerHandler.CREATE_ZONE;
        msg.setData(b);
        mHandler.sendMessage(msg);
    }

    private boolean createZoneWithPolicies (String zoneName, String policyList)
    {
        synchronized (zonedbLock) {
            final SQLiteDatabase db = openHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                ContentValues values = new ContentValues();

                values.put(ZONES_NAME, zoneName);

                long appzoneId = db.insert(TABLE_ZONES, null, values);

                if (appzoneId < 0) {
                    Log.w(TAG, "Log SHAH insertZoneIntoDatabase: " + zoneName
                            + ", skipping the DB insert failed");
                    return false;
                }

                int zone_id = getZoneID(zoneName);
                values.remove(ZONES_NAME);
                values.put(ZONEPOLICIES_ZONE_ID, zone_id);

                String policyIdList = "";
                for(String ss:policyIdList.split(";"))
                {
                    if(!ss.isEmpty())
                        policyIdList += getPolicyID(ss) + ";";
                }

                values.put(ZONEPOLICIES_POLICYLIST, policyIdList);

                appzoneId = db.insert(TABLE_ZONEPOLICIES, null, values);

                if (appzoneId < 0) {
                    Log.w(TAG, "Log SHAH insert Zone policies Into Database: zone id: " + zone_id + " zonename: " + zoneName
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

    //param optional here, can receive null/either new name, or new policies
    public void editZone(String zoneName, String action, String paramList) {
        Log.i(TAG, "Log SHAH edit Zone " + zoneName + "with action " + action);

        // Creating Bundle object
        Bundle b = new Bundle();

        // Storing data into bundle
        b.putString("zonename", zoneName);
        b.putString("action", action);
        b.putString("paramlist", paramList);

        Message msg = Message.obtain();
        msg.what = ExecutionZoneWorkerHandler.EDIT_ZONE;
        msg.setData(b);
        mHandler.sendMessage(msg);
    }

    private boolean editZoneOrPolicies (String zoneName, String action, String paramList)
    {
        synchronized (zonedbLock) {
            final SQLiteDatabase db = openHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                ContentValues values = new ContentValues();
                int zone_id = getZoneID(zoneName);
                long appzoneId = 0;

                if(action.toUpperCase().equals("RENAME")) {
                    values.put(ZONES_NAME, paramList);

                    appzoneId = db.update(TABLE_ZONES, values, ZONES_ID + "=" +zone_id, null);

                    if (appzoneId < 0) {
                        Log.w(TAG, "Log SHAH update Zone rename Into Database: " + zoneName
                                + ", skipping the DB update, failed");
                        return false;
                    }
                }
                else if(action.toUpperCase().equals("DELETE"))
                {
                    long isZoneEmpty = DatabaseUtils.longForQuery(db,
                            "select count(*)  from " + TABLE_APPZONES
                                    + " WHERE " + APPZONES_ZONE_ID + "=?",
                            new String[]{zone_id+""});

                    if(isZoneEmpty > 0)
                    {
                        Log.w(TAG, "Log SHAH delete Zone in Database: " + zoneName
                                + ", skipping the DB update, failed, zone not empty");
                        return false;
                    }
                    else
                    {
                        if(db.delete(TABLE_ZONES,ZONES_ID+"="+zone_id,null)!=1)
                        {
                            Log.w(TAG, "Log SHAH delete Zone in zones Database: " + zoneName
                                    + ", skipping the DB update, failed");
                            return false;
                        }
                        if(db.delete(TABLE_ZONEPOLICIES,ZONEPOLICIES_ZONE_ID+"="+zone_id,null)!=1)
                        {
                            Log.w(TAG, "Log SHAH delete Zone in zonepolicies Database: " + zoneName
                                    + ", skipping the DB update, failed");
                            return false;
                        }

                    }

                }
                else if(action.toUpperCase().equals("UPDATEPOLICY")) //a new set of policy for the zone, full set must be given from the app
                {
                    values.put(ZONEPOLICIES_ZONE_ID, zone_id);

                    String policyIdList = "";
                    for(String ss:paramList.split(";"))
                    {
                        if(!ss.isEmpty())
                            policyIdList += getPolicyID(ss) + ";";
                    }

                    values.put(ZONEPOLICIES_POLICYLIST, policyIdList);

                    appzoneId = db.update(TABLE_ZONEPOLICIES, values, ZONEPOLICIES_ZONE_ID + "=" +zone_id, null);

                    if (appzoneId < 0) {
                        Log.w(TAG, "Log SHAH update Zone policies Into Database: zone id: " + zone_id + " zonename: " + zoneName
                                + ", skipping the DB update failed");
                        return false;
                    }
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

        }

        return true;
    }

    public void createPolicy(String policyName, String ruleList) {
        Log.i(TAG, "Log SHAH create policy " + policyName + "with rules " + ruleList);

        // Creating Bundle object
        Bundle b = new Bundle();

        // Storing data into bundle
        b.putString("policyname", policyName);
        b.putString("rulelist", ruleList);

        Message msg = Message.obtain();
        msg.what = ExecutionZoneWorkerHandler.CREATE_POLICY;
        msg.setData(b);
        mHandler.sendMessage(msg);
    }

    private boolean createPolicyInDB (String policyName, String ruleList)
    {
        synchronized (zonedbLock) {
            final SQLiteDatabase db = openHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                ContentValues values = new ContentValues();

                values.put(POLICIES_NAME, policyName);
                values.put(POLICIES_RULES, ruleList);

                long apppolicy = db.insert(TABLE_POLICIES, null, values);

                if (apppolicy < 0) {
                    Log.w(TAG, "Log SHAH insert policy Into Database: " + policyName
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

    public void setPolicy(String policyName, String zoneName) {
        Log.i(TAG, "Log SHAH set policy " + policyName + "to zone " + zoneName);

        // Creating Bundle object
        Bundle b = new Bundle();

        // Storing data into bundle
        b.putString("policyname", policyName);
        b.putString("zonename", zoneName);

        Message msg = Message.obtain();
        msg.what = ExecutionZoneWorkerHandler.SET_POLICY;
        msg.setData(b);
        mHandler.sendMessage(msg);
    }

    private boolean setPolicyToZone (String policyName, String zoneName)
    {
        synchronized (zonedbLock) {
            final SQLiteDatabase db = openHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                int zone_id = getZoneID(zoneName);
                int policy_id = getPolicyID(policyName);

                ContentValues values = new ContentValues();

                String policyList = DatabaseUtils.stringForQuery(db,
                        "select "+ ZONEPOLICIES_POLICYLIST +" from " + TABLE_ZONEPOLICIES
                                + " WHERE " + ZONEPOLICIES_ZONE_ID + "=?",
                        new String[]{zone_id+""});

                if(policyList.contains(";"+policy_id+";"))
                {
                    Log.w(TAG, "Log SHAH set policy to zone  in Database: zoneid: " + zone_id + " policy :"
                            + policy_id+" : already exists, skipping the DB update failed");
                    return false;
                }

                values.put(ZONEPOLICIES_POLICYLIST, policyList+policy_id+";");

                long apppolicy = db.update(TABLE_ZONEPOLICIES, values, ZONEPOLICIES_ZONE_ID + "=" + zone_id, null);

                if (apppolicy < 0) {
                    Log.w(TAG, "Log SHAH set policy to zone  in Database: zoneid: " + zone_id + " policy :"
                            +policy_id+ ", skipping the DB update failed");
                    return false;
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

        }

        return true;
    }


    //param optional here, can receive null/either new name, or new policies
    public void editPolicy(String policyName, String action, String paramList) {
        Log.i(TAG, "Log SHAH edit policy " + policyName + " with action " + action);

        // Creating Bundle object
        Bundle b = new Bundle();

        // Storing data into bundle
        b.putString("policyname", policyName);
        b.putString("action", action);
        b.putString("paramlist", paramList);

        Message msg = Message.obtain();
        msg.what = ExecutionZoneWorkerHandler.EDIT_POLICY;
        msg.setData(b);
        mHandler.sendMessage(msg);
    }

    private boolean editPoliciesInDB (String policyName, String action, String paramList)
    {
        synchronized (zonedbLock) {
            final SQLiteDatabase db = openHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                ContentValues values = new ContentValues();
                int policy_id = getPolicyID(policyName);
                long appzoneId = 0;

                if(action.toUpperCase().equals("RENAME")) {
                    values.put(POLICIES_NAME, paramList);

                    appzoneId = db.update(TABLE_POLICIES, values, POLICIES_ID + "=" +policy_id, null);

                    if (appzoneId < 0) {
                        Log.w(TAG, "Log SHAH update policy rename Into Database: policy name:" + policyName
                                + ", skipping the DB update, failed");
                        return false;
                    }
                }
                else if(action.toUpperCase().equals("DELETE"))
                {
                    Log.d(TAG,"Deleting policy: "+policyName+" with id: "+policy_id+" checking isZoneEmpty");
                    long isZoneEmpty = 0;
                    try {
                        isZoneEmpty = DatabaseUtils.queryNumEntries(db, TABLE_ZONEPOLICIES, ZONEPOLICIES_POLICYLIST + " LIKE ?", new String[]{"%;" + policy_id + ";%"});
                        Log.d(TAG,"Deleting policy: "+policyName+" with id: "+policy_id+" checking isZoneEmpty: value: "+isZoneEmpty);
                    }
                    catch (Exception e)
                    {
                        Log.e(TAG,"Error in queryNumEntries in deleting policy");
                    }

                    if(isZoneEmpty > 0)
                    {
                        Log.w(TAG, "Log SHAH delete policy in Database: " + policyName
                                + ", skipping the DB update, failed, policy is used");
                        return false;
                    }
                    else
                    {
                        if(db.delete(TABLE_POLICIES,POLICIES_ID+"="+policy_id,null)!=1)
                        {
                            Log.w(TAG, "Log SHAH delete policy in Database: " + policyName
                                    + ", skipping the DB update, failed");
                            return false;
                        }

                    }

                }
                else if(action.toUpperCase().equals("UPDATEPOLICY")) //a new set of RULES FOR THE policy
                {
                    values.put(POLICIES_ID, policy_id);


                    values.put(POLICIES_RULES, paramList);

                    appzoneId = db.update(TABLE_POLICIES, values, POLICIES_ID + "=" +policy_id, null);

                    if (appzoneId < 0) {
                        Log.w(TAG, "Log SHAH update policy Into Database: policy id: " + policy_id + " policyname: " + policyName
                                + ", skipping the DB update failed");
                        return false;
                    }
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

        }

        return true;
    }

    public String[] getAllZones()
    {
        final SQLiteDatabase db = openHelper.getReadableDatabase();
        String[] zones = null;

        try {
            Cursor c = db.rawQuery("SELECT " + ZONES_NAME + " FROM " + TABLE_ZONES, null);
            zones = new String[c.getCount()];
            int i = 0;
            if(c.moveToFirst()){
                do{
                    //assing values
                    zones[i++] = c.getString(0);

                }while(c.moveToNext());
            }
            c.close();
        }
        catch (Exception e) {
            // Log, don't crash!
            Log.e(TAG, "Log SHAH Exception in getAllZones, message: "+e.getMessage());
        }

        db.close();

        return zones;

    }
    public String[] getAllPolicies()
    {
        final SQLiteDatabase db = openHelper.getReadableDatabase();
        String[] policies = null;

        try {
            Cursor c = db.rawQuery("SELECT " + POLICIES_NAME + " FROM " + TABLE_POLICIES, null);
            policies = new String[c.getCount()];
            int i = 0;
            if(c.moveToFirst()){
                do{
                    //assing values
                    policies[i++] = c.getString(0);

                }while(c.moveToNext());
            }
            c.close();
        }
        catch (Exception e) {
            // Log, don't crash!
            Log.e(TAG, "Log SHAH Exception in getAllPolicies, message: "+e.getMessage());
        }

        db.close();

        return policies;
    }
    public String getRulesOfPolicy(String policyname)
    {
        final SQLiteDatabase db = openHelper.getReadableDatabase();
        String rules = null;

        try {
            rules = DatabaseUtils.stringForQuery(db,"SELECT " + POLICIES_RULES + " FROM " + TABLE_POLICIES
                            + " WHERE " + POLICIES_NAME + "=?",
                    new String[]{policyname});
        }
        catch (Exception e) {
            // Log, don't crash!
            Log.e(TAG, "Log SHAH Exception in getRulesOfPolicy, message: "+e.getMessage());
        }

        db.close();

        return rules;
    }
    public String getZoneOfApp(String packagename)
    {
        final SQLiteDatabase db = openHelper.getReadableDatabase();
        String zonename = null;

        try {
            zonename = DatabaseUtils.stringForQuery(db,"SELECT Z." + ZONES_NAME + " FROM " + TABLE_ZONES
                            + " Z INNER JOIN "+TABLE_APPZONES+" AZ ON Z." + ZONES_ID + "=AZ." + APPZONES_ZONE_ID + " WHERE AZ." + APPZONES_APP_NAME + "=?",
                    new String[]{packagename});
        }
        catch (Exception e) {
            // Log, don't crash!
            Log.e(TAG, "Log SHAH Exception in getZoneOfApp, message: "+e.getMessage());
        }

        db.close();

        return zonename;

    }
    public String[] getPoliciesOfZone(String zonename) {
        final SQLiteDatabase db = openHelper.getReadableDatabase();
        List<String> policies = new ArrayList<String>();
        String policylist = null;
        String tmpPolicyName= null;

        try {
            policylist = DatabaseUtils.stringForQuery(db,"SELECT ZP." + ZONEPOLICIES_POLICYLIST + " FROM " + TABLE_ZONES
                            + " Z INNER JOIN "+TABLE_ZONEPOLICIES+" ZP ON Z." + ZONES_ID + "=ZP." + ZONEPOLICIES_ZONE_ID + " WHERE Z." + ZONES_NAME + "=?",
                    new String[]{zonename});
        }
        catch (Exception e) {
            // Log, don't crash!
            Log.e(TAG, "Log SHAH Exception in getPoliciesOfZone when fetching policylist, message: "+e.getMessage());
        }

        for(String policyid: policylist.split(";")) {
            if(policyid.isEmpty()==false) {
                try {
                     tmpPolicyName = DatabaseUtils.stringForQuery(db,"SELECT " + POLICIES_NAME + " FROM " + TABLE_POLICIES
                                    + " WHERE " + POLICIES_ID + "=?",
                            new String[]{policyid.trim()});
                }
                catch (Exception e) {
                    // Log, don't crash!
                    Log.e(TAG, "Log SHAH Exception in getPoliciesOfZone when fetching policy name from id, message: "+e.getMessage());
                }

                policies.add(tmpPolicyName);
            }
        }

        db.close();

        String[] policyNames = new String[policies.size()];
        return policies.toArray(policyNames);
    }

    public Map<String,String> getPoliciesOfZoneWithRules(String zonename) {
        final SQLiteDatabase db = openHelper.getReadableDatabase();
        Map<String,String> policiesAndRules = new HashMap<String, String>();
        String policylist = null;


        try {
            policylist = DatabaseUtils.stringForQuery(db,"SELECT ZP." + ZONEPOLICIES_POLICYLIST + " FROM " + TABLE_ZONES
                            + " Z INNER JOIN "+TABLE_ZONEPOLICIES+" ZP ON Z." + ZONES_ID + "=ZP." + ZONEPOLICIES_ZONE_ID + " WHERE Z." + ZONES_NAME + "=?",
                    new String[]{zonename});
        }
        catch (Exception e) {
            // Log, don't crash!
            Log.e(TAG, "Log SHAH Exception in getPoliciesOfZoneWithRules when fetching policylist, message: "+e.getMessage());
        }

        for(String policyid: policylist.split(";")) {
            if(policyid.isEmpty()==false) {
                try {
                    Cursor cur = db.rawQuery("SELECT " + POLICIES_NAME + ","+POLICIES_RULES+" FROM " + TABLE_POLICIES
                                    + " WHERE " + POLICIES_ID + "=?",
                            new String[]{policyid.trim()});

                    if (cur != null) {
                        if (cur.moveToFirst()) {
                            do {
                                policiesAndRules.put(cur.getString(0),cur.getString(1));
                            } while (cur.moveToNext());
                        }
                    }

                }
                catch (Exception e) {
                    // Log, don't crash!
                    Log.e(TAG, "Log SHAH Exception in getPoliciesOfZoneWithRules when fetching policy name from id, message: "+e.getMessage());
                }
            }
        }

        db.close();


        return policiesAndRules;
    }


    public int checkZonePermission(String permission, int uid) {
        Log.i(TAG, "Log SHAH check zone permission " + permission + "of uid " + uid);


        PackageManager pm = mContext.getPackageManager();
        String[] packages = pm.getPackagesForUid(uid);

        for(String packageName: packages)
        {
            if(checkZonePermission(permission, packageName)==PERMISSION_NOT_PERMITTED_IN_ZONE)
                return PackageManager.PERMISSION_DENIED;
        }

        return PackageManager.PERMISSION_GRANTED;
    }

    private int checkZonePermission (String permission, String packageName)
    {
            try {
                //a zone can have multiple policies, each policy can have multiple rules separated by ;
                Map<String,String> policyRules = getPoliciesOfZoneWithRules(getZoneOfApp(packageName));

                Set<String> rules = new HashSet<String>();

                if(DEBUG_ENABLE)
                    Log.d(TAG,"SHAH in checkzonepermission permission: "+permission+" packagename: "+packageName );


                for(String value:policyRules.values()) {
                    String []tmpRules = value.split(";"); //separating rules of a policy

                    for(String ss: tmpRules)
                    {
                        String []tmpRuleParams = ss.split(",");
                        if(tmpRuleParams[1].equals(permission))
                        {
                            rules.add(tmpRuleParams[2]+"{"+tmpRuleParams[3]+"}"); //adding to hashset in a format [TIME_ALWAYS+PHONENUMBER_3433333599]{DENY}
                        }
                    }
                }

                for(String check: rules)//only allows time for now, phone number will be added later
                {
                    String time = check.substring(check.indexOf('['),check.lastIndexOf(']'));
                    String allowordeny = check.substring(check.indexOf('{',check.lastIndexOf(']')+1),check.lastIndexOf('}'));

                    String []startendTime = time.split("_");

                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
                    Date startTime = simpleDateFormat.parse(startendTime[1]);
                    Date endTime = simpleDateFormat.parse(startendTime[2]);

                    if(DEBUG_ENABLE)
                        Log.d(TAG,"SHAH in checkzonepermission current rule: "+check+" parsed starttime: "+startTime.toString()+" endtime:"+endTime.toString());

                    Date now = new Date();

                    if (startTime.before(now) && endTime.after(now)) {
                        if(allowordeny.equals("DENY"))
                            return PERMISSION_NOT_PERMITTED_IN_ZONE;

                    }

                }
            } catch (Exception e)
            {
                Log.e(TAG, "Log SHAH something bad happened man, in checkzonepermission in executionzoneservice, exception: "+e.getMessage());
            }

        return PERMISSION_PERMITTED_IN_ZONE;
    }

    private int getZoneID (String zoneName)
    {
        long zone_id_long = -1111;

        final SQLiteDatabase db = openHelper.getReadableDatabase();

        try {
            zone_id_long = DatabaseUtils.longForQuery(db,
                    "select "+ ZONES_ID +" from " + TABLE_ZONES
                            + " WHERE " + ZONES_NAME + "=?",
                    new String[]{zoneName});
        } catch (Exception e) {
            // Log, don't crash!
            Log.e(TAG, "Log SHAH Exception in getZoneID");
            zone_id_long = -11111;
        }

        return (int) zone_id_long;
    }

    private int getPolicyID (String policyName)
    {
        long policy_id_long = -1111;

        final SQLiteDatabase db = openHelper.getReadableDatabase();

        try {
            policy_id_long = DatabaseUtils.longForQuery(db,
                    "select "+ POLICIES_ID +" from " + TABLE_POLICIES
                            + " WHERE " + POLICIES_NAME + "=?",
                    new String[]{policyName});


        } catch (Exception e) {
            // Log, don't crash!
            Log.e(TAG, "Log SHAH Exception in getPolicyID");
            policy_id_long = -11111;
        }

        return (int) policy_id_long;
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

        return "<DEFAULT,READ_CALENDAR,[TIME_ALWAYS],DENY>;" +
                "<DEFAULT,WRITE_CALENDAR,[TIME_ALWAYS],DENY>;" +
                "<DEFAULT,CAMERA,[TIME_ALWAYS],DENY>;" +
                "<DEFAULT,READ_CONTACTS,[TIME_ALWAYS],DENY>;" +
                "<DEFAULT,WRITE_CONTACTS,[TIME_ALWAYS],DENY>;" +
                "<DEFAULT,GET_ACCOUNTS,[TIME_ALWAYS],DENY>;" +
                "<DEFAULT,ACCESS_FINE_LOCATION,[TIME_ALWAYS],DENY>;" +
                "<DEFAULT,ACCESS_COARSE_LOCATION,[TIME_ALWAYS],DENY>;" +
                "<DEFAULT,RECORD_AUDIO,[TIME_ALWAYS],DENY>;" +
                "<DEFAULT,READ_PHONE_STATE,[TIME_ALWAYS],DENY>;" +
                "<DEFAULT,CALL_PHONE,[TIME_ALWAYS],DENY>;" +
                "<DEFAULT,READ_CALL_LOG,[TIME_ALWAYS],DENY>;" +
                "<DEFAULT,WRITE_CALL_LOG,[TIME_ALWAYS],DENY>;" +
                "<DEFAULT,ADD_VOICEMAIL,[TIME_ALWAYS],DENY>;" +
                "<DEFAULT,USE_SIP,[TIME_ALWAYS],DENY>;" +
                "<DEFAULT,PROCESS_OUTGOING_CALLS,[TIME_ALWAYS],DENY>;" +
                "<DEFAULT,BODY_SENSORS,[TIME_ALWAYS],DENY>;" +
                "<DEFAULT,SEND_SMS,[TIME_ALWAYS],DENY>;" +
                "<DEFAULT,RECEIVE_SMS,[TIME_ALWAYS],DENY>;" +
                "<DEFAULT,READ_SMS,[TIME_ALWAYS],DENY>;" +
                "<DEFAULT,RECEIVE_WAP_PUSH,[TIME_ALWAYS],DENY>;" +
                "<DEFAULT,RECEIVE_MMS,[TIME_ALWAYS],DENY>;" +
                "<DEFAULT,READ_EXTERNAL_STORAGE,[TIME_ALWAYS],DENY>;" +
                "<DEFAULT,WRITE_EXTERNAL_STORAGE,[TIME_ALWAYS],DENY>;";
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

                db.execSQL("INSERT OR REPLACE INTO " + TABLE_ZONES + " ( "
                        + ZONES_NAME + ") "
                        + " VALUES ( "
                        + "'UNINSTALLED' );");
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
                        + "UNIQUE (" + ZONEPOLICIES_ZONE_ID + "))");

                //add default zone policy mappings

                c = db.rawQuery("select " + ZONES_ID + " from " + TABLE_ZONES
                                + " WHERE " + ZONES_NAME + "=?",
                        new String[]{"TRUSTED"});

                if (c.getCount() > 0) {
                    c.moveToFirst();

                    zoneID = c.getInt(0);
                }

                db.execSQL("INSERT OR REPLACE INTO " + TABLE_ZONEPOLICIES + " (  "
                        + ZONEPOLICIES_ZONE_ID + ", "
                        + ZONEPOLICIES_POLICYLIST + ") "
                        + " VALUES ( "
                        + zoneID
                        + ", ''"
                        + ");");

                c = db.rawQuery("select " + ZONES_ID + " from " + TABLE_ZONES
                                + " WHERE " + ZONES_NAME + "=?",
                        new String[]{"UNINSTALLED"});

                if (c.getCount() > 0) {
                    c.moveToFirst();

                    zoneID = c.getInt(0);
                }

                db.execSQL("INSERT OR REPLACE INTO " + TABLE_ZONEPOLICIES + " (  "
                        + ZONEPOLICIES_ZONE_ID + ", "
                        + ZONEPOLICIES_POLICYLIST + ") "
                        + " VALUES ( "
                        + zoneID
                        + ", ''"
                        + ");");

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
                        + ", '" + policyID + ";'"
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
                        + ", '" + policyID + ";'"
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
                        + ", '" + policyID + ";'"
                        + ");");
            }
            catch (Exception once)
            {
                Log.e(TAG, "Log SHAH Exception occurred while creating zones.db. The exception message is; "+ once.getMessage());
            }
        }
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.e(TAG, "Log SHAH upgrade from version " + oldVersion + " to version " + newVersion);

            if (oldVersion < newVersion) {
                // no longer need to do anything since the work is done
                // when upgrading from version 2
                oldVersion = newVersion;
            }


            if (oldVersion != newVersion) {
                Log.e(TAG, "Log SHAH failed to upgrade version " + oldVersion + " to version " + newVersion);
            }
        }

        @Override
        public void onOpen(SQLiteDatabase db) {
            if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "Log SHAH opened database " + DATABASE_NAME);
        }
    }



}
