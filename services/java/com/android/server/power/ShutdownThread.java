/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

 
package com.android.server.power;

import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.IActivityManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.IBluetoothManager;
import android.nfc.NfcAdapter;
import android.nfc.INfcAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.Vibrator;
import android.os.SystemVibrator;
import android.os.storage.IMountService;
import android.os.storage.IMountShutdownObserver;

import com.android.internal.telephony.ITelephony;
import java.io.File;
import android.util.Log;
import android.view.WindowManager;
import android.content.DialogInterface.OnDismissListener;

import android.util.Slog;
import java.util.ArrayList;
import java.util.List;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.app.IWallpaperManager;
import android.app.WallpaperInfo;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.pm.ComponentInfo;
import android.os.Process;
import android.text.TextUtils.SimpleStringSplitter;
import android.content.ComponentName;
import android.text.TextUtils;
import java.io.FileOutputStream;
import java.io.IOException;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;

public final class ShutdownThread extends Thread {
    // constants
    private static final String TAG = "ShutdownThread";
    private static final int PHONE_STATE_POLL_SLEEP_MSEC = 500;
    // maximum time we wait for the shutdown broadcast before going on.
    private static final int QB_MAX_BROADCAST_TIME = 3*1000;
    private static final int MAX_BROADCAST_TIME = 10*1000;
    private static final int MAX_SHUTDOWN_WAIT_TIME = 20*1000;
    private static final int MAX_RADIO_WAIT_TIME = 12*1000;

    // length of vibration before shutting down
    private static final int SHUTDOWN_VIBRATE_MS = 500;
    
    // state tracking
    private static Object sIsStartedGuard = new Object();
    private static boolean sIsStarted = false;
    
    private static boolean mReboot;
    private static boolean mRebootSafeMode;
    private static String mRebootReason;

    // Provides shutdown assurance in case the system_server is killed
    public static final String SHUTDOWN_ACTION_PROPERTY = "sys.shutdown.requested";

    // Indicates whether we are rebooting into safe mode
    public static final String REBOOT_SAFEMODE_PROPERTY = "persist.sys.safemode";

    // static instance of this thread
    private static ShutdownThread sInstance = new ShutdownThread();
    
    private final Object mActionDoneSync = new Object();
    private boolean mActionDone;
    private Context mContext;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mCpuWakeLock;
    private PowerManager.WakeLock mScreenWakeLock;
    private Handler mHandler;

    private static AlertDialog sConfirmDialog;
   
    //quick boot
    private static final int NORMAL_SHUTDOWN_FLOW = 0;
    private static final int QB_SHUTDOWN_FLOW = 1;
    private static int mShutdownFlow = NORMAL_SHUTDOWN_FLOW;
    private static ProgressDialog pd = null;
    //private static Object mShutdownThreadSync = new Object();
    static final ArrayList<String> mShutdownWhiteList = new ArrayList();
    static final String[] mHardCodeShutdownList = new String[] { "system", "com.android.phone", "android.process.acore", "com.android.wallpaper", "com.android.systemui" };
    private static final String QB_ENABLE_PROPERTY = "ro.quickboot.enable";

    private ShutdownThread() {
    }
 
    /**
     * Request a clean shutdown, waiting for subsystems to clean up their
     * state etc.  Must be called from a Looper thread in which its UI
     * is shown.
     *
     * @param context Context used to display the shutdown progress dialog.
     * @param confirm true if user confirmation is needed before shutting down.
     */
    public static void shutdown(final Context context, boolean confirm) {
        mReboot = false;
        mRebootSafeMode = false;
        shutdownInner(context, confirm);
    }

    static void shutdownInner(final Context context, boolean confirm) {
        // ensure that only one thread is trying to power down.
        // any additional calls are just returned
        synchronized (sIsStartedGuard) {
            if (sIsStarted) {
                Log.d(TAG, "Request to shutdown already running, returning.");
                return;
            }
        }

        final int longPressBehavior = context.getResources().getInteger(
                        com.android.internal.R.integer.config_longPressOnPowerBehavior);
        /*final*/ int resourceId = mRebootSafeMode
                ? com.android.internal.R.string.reboot_safemode_confirm
                : (longPressBehavior == 2
                        ? com.android.internal.R.string.shutdown_confirm_question
                        : com.android.internal.R.string.shutdown_confirm);

        if(SystemProperties.getBoolean("sys.chiptemp.enable", false))
            resourceId = com.android.internal.R.string.shutdown_confirm_question_chiptemp_hot;
        

        Log.d(TAG, "Notifying thread to start shutdown longPressBehavior=" + longPressBehavior);

        if (confirm) {
            final CloseDialogReceiver closer = new CloseDialogReceiver(context);
            if (sConfirmDialog != null) {
                sConfirmDialog.dismiss();
            }

	    String[] array = new String[] { context.getString(com.android.internal.R.string.quick_boot_select_tip) };
	    //String[] array = new String[] { "quick boot?" };
 	    boolean[] selected = new boolean[] { isQuickBoot(context) };
	    AlertDialog.Builder ab = new AlertDialog.Builder(context)
                    .setTitle(mRebootSafeMode
                            ? com.android.internal.R.string.reboot_safemode_title
                            : com.android.internal.R.string.power_off)
                    .setPositiveButton(com.android.internal.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            beginShutdownSequence(context);
                        }
                    })
                    .setNegativeButton(com.android.internal.R.string.no, null);
	    if( mRebootSafeMode || !SystemProperties.getBoolean(QB_ENABLE_PROPERTY, true) ) {
		ab.setMessage(resourceId);
	    } else {
		ab.setMultiChoiceItems(array, selected, 
		new DialogInterface.OnMultiChoiceClickListener(){
            		@Override
            		public void onClick(DialogInterface dialog, int which,boolean isChecked) {
				Log.i(TAG, "qb select isChecked:" + isChecked);
				Settings.Secure.putInt(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_QUICK_BOOT, isChecked ? 1 : 0);
            		}
        	});
	    }
	    sConfirmDialog = ab.create();

            closer.dialog = sConfirmDialog;
            sConfirmDialog.setOnDismissListener(closer);
            sConfirmDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
            sConfirmDialog.show();

            sConfirmDialog.setOnDismissListener(new OnDismissListener(){
    			public void onDismiss(DialogInterface dialog) {
                    SystemProperties.set("sys.chiptemp.enable", "false");
    			}
    		});
        } else {
            beginShutdownSequence(context);
        }
    }

    private static class CloseDialogReceiver extends BroadcastReceiver
            implements DialogInterface.OnDismissListener {
        private Context mContext;
        public Dialog dialog;

        CloseDialogReceiver(Context context) {
            mContext = context;
            IntentFilter filter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.registerReceiver(this, filter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            dialog.cancel();
        }

        public void onDismiss(DialogInterface unused) {
            mContext.unregisterReceiver(this);
        }
    }

    /**
     * Request a clean shutdown, waiting for subsystems to clean up their
     * state etc.  Must be called from a Looper thread in which its UI
     * is shown.
     *
     * @param context Context used to display the shutdown progress dialog.
     * @param reason code to pass to the kernel (e.g. "recovery"), or null.
     * @param confirm true if user confirmation is needed before shutting down.
     */
    public static void reboot(final Context context, String reason, boolean confirm) {
        mReboot = true;
        mRebootSafeMode = false;
        mRebootReason = reason;
        shutdownInner(context, confirm);
    }

    /**
     * Request a reboot into safe mode.  Must be called from a Looper thread in which its UI
     * is shown.
     *
     * @param context Context used to display the shutdown progress dialog.
     * @param confirm true if user confirmation is needed before shutting down.
     */
    public static void rebootSafeMode(final Context context, boolean confirm) {
        mReboot = true;
        mRebootSafeMode = true;
        mRebootReason = null;
        shutdownInner(context, confirm);
    }

    private static void beginShutdownSequence(Context context) {
        synchronized (sIsStartedGuard) {
            if (sIsStarted) {
                Log.d(TAG, "Shutdown sequence already running, returning.");
                return;
            }
            sIsStarted = true;

	    sInstance = new ShutdownThread();
        }
				boolean showShutdownAnim = new File("/system/media/shutdownanimation.zip").exists();
    if (showShutdownAnim) {
        Log.d(TAG, "shutdownanim");
         android.os.SystemProperties.set("service.bootanim.exit", "0");
        android.os.SystemProperties.set("ctl.start", "shutdownanim");
    } else {
        // throw up an indeterminate system dialog to indicate radio is
        // shutting down.
        pd = new ProgressDialog(context);
        pd.setTitle(context.getText(com.android.internal.R.string.power_off));
        pd.setMessage(context.getText(com.android.internal.R.string.shutdown_progress));
        pd.setIndeterminate(true);
        pd.setCancelable(false);
        pd.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);

        pd.show();
			}		

        sInstance.mContext = context;
        sInstance.mPowerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);

        // make sure we never fall asleep again
        sInstance.mCpuWakeLock = null;
        try {
            sInstance.mCpuWakeLock = sInstance.mPowerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK, TAG + "-cpu");
            sInstance.mCpuWakeLock.setReferenceCounted(false);
            sInstance.mCpuWakeLock.acquire();
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to acquire wake lock", e);
            sInstance.mCpuWakeLock = null;
        }

        // also make sure the screen stays on for better user experience
        sInstance.mScreenWakeLock = null;
        if (sInstance.mPowerManager.isScreenOn()) {
            try {
                sInstance.mScreenWakeLock = sInstance.mPowerManager.newWakeLock(
                        PowerManager.FULL_WAKE_LOCK, TAG + "-screen");
                sInstance.mScreenWakeLock.setReferenceCounted(false);
                sInstance.mScreenWakeLock.acquire();
            } catch (SecurityException e) {
                Log.w(TAG, "No permission to acquire wake lock", e);
                sInstance.mScreenWakeLock = null;
            }
        }

        // start the thread that initiates shutdown
        sInstance.mHandler = new Handler() {
        };

/*	if (sInstance.getState() != Thread.State.NEW || sInstance.isAlive()) {
            if (mShutdownFlow == QB_SHUTDOWN_FLOW) {
                Log.d(TAG, "ShutdownThread exists already");
                //checkShutdownFlow();
                synchronized (mShutdownThreadSync) {
                    mShutdownThreadSync.notify();
                }
            } else {
                Log.e(TAG, "Thread state is not normal! froce to shutdown!");
                //delayForPlayAnimation();
                //SystemProperties.set("ctl.start", "shutdown");
            }
        } else {*/
	/*if (sInstance.getState() != Thread.State.NEW || sInstance.isAlive()) {
		sInstance.stop();
	}*/
        sInstance.start();
	//}
    }

    void actionDone() {
        synchronized (mActionDoneSync) {
            mActionDone = true;
            mActionDoneSync.notifyAll();
        }
    }

    /**
     * Makes sure we handle the shutdown gracefully.
     * Shuts off power regardless of radio and bluetooth state if the alloted time has passed.
     */
    public void run() {
	checkShutdownFlow();

        BroadcastReceiver br = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                // We don't allow apps to cancel this, so ignore the result.
                actionDone();
            }
        };
        if(new File("/system/media/shutdownanimation.zip").exists()){
        try{
					  Thread.sleep(5000);					       
					}catch(InterruptedException ex){
				}	
			}	

        /*
         * Write a system property in case the system_server reboots before we
         * get to the actual hardware restart. If that happens, we'll retry at
         * the beginning of the SystemServer startup.
         */
        {
            String reason = (mReboot ? "1" : "0") + (mRebootReason != null ? mRebootReason : "");
            SystemProperties.set(SHUTDOWN_ACTION_PROPERTY, reason);
        }

        /*
         * If we are rebooting into safe mode, write a system property
         * indicating so.
         */
        if (mRebootSafeMode) {
            SystemProperties.set(REBOOT_SAFEMODE_PROPERTY, "1");
        }

        Log.i(TAG, "Sending shutdown broadcast...");
        
        // First send the high-level shut down broadcast.
        mActionDone = false;
        Intent intent = new Intent(Intent.ACTION_SHUTDOWN);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
	intent.putExtra("_mode", mShutdownFlow);
        mContext.sendOrderedBroadcastAsUser(intent,
                UserHandle.ALL, null, br, mHandler, 0, null, null);
       
	int nBroadcastTimeout =  ( (mShutdownFlow == QB_SHUTDOWN_FLOW) ? QB_MAX_BROADCAST_TIME : MAX_BROADCAST_TIME ); 
        final long endTime = SystemClock.elapsedRealtime() + nBroadcastTimeout;
        synchronized (mActionDoneSync) {
            while (!mActionDone) {
                long delay = endTime - SystemClock.elapsedRealtime();
                if (delay <= 0) {
                    Log.w(TAG, "Shutdown broadcast timed out");
                    //mShutdownFlow = NORMAL_SHUTDOWN_FLOW;
                    break;
                }
                try {
                    mActionDoneSync.wait(delay);
                } catch (InterruptedException e) {
                }
            }
        }
      
	// Also send ACTION_SHUTDOWN_QB in QB shut down flow
        if (mShutdownFlow == QB_SHUTDOWN_FLOW) {
            mActionDone = false;
            mContext.sendOrderedBroadcast(new Intent("android.intent.action.ACTION_SHUTDOWN_QB"), null,
                    br, mHandler, 0, null, null);
            final long endTimeQB = SystemClock.elapsedRealtime() + nBroadcastTimeout;
            synchronized (mActionDoneSync) {
                while (!mActionDone) {
                    long delay = endTimeQB - SystemClock.elapsedRealtime();
                    if (delay <= 0) {
                        Log.w(TAG, "Shutdown broadcast ACTION_SHUTDOWN_QB timed out");
                        if (mShutdownFlow == QB_SHUTDOWN_FLOW) {
                            Log.d(TAG, "change shutdown flow from ipo to normal: ACTION_SHUTDOWN_QB timeout");
                            //mShutdownFlow = NORMAL_SHUTDOWN_FLOW;
                        }
                        break;
                    }
                    try {
                        mActionDoneSync.wait(delay);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
 
        if( mShutdownFlow == NORMAL_SHUTDOWN_FLOW ) { 
            Log.i(TAG, "Shutting down activity manager...");
        
            final IActivityManager am =
                ActivityManagerNative.asInterface(ServiceManager.checkService("activity"));
            if (am != null) {
                try {
                    am.shutdown(MAX_BROADCAST_TIME);
                } catch (RemoteException e) {
                }
            } 
	} else {
            Log.i( TAG, "Quickboot bypass shutdown ActivityManagerService");
        }

        // Shutdown radios.
        shutdownRadios(MAX_RADIO_WAIT_TIME);

        // Shutdown MountService to ensure media is in a safe state
        IMountShutdownObserver observer = new IMountShutdownObserver.Stub() {
            public void onShutDownComplete(int statusCode) throws RemoteException {
                Log.w(TAG, "Result code " + statusCode + " from MountService.shutdown");
                actionDone();
            }
        };

	if( mShutdownFlow == NORMAL_SHUTDOWN_FLOW ) {
        Log.i(TAG, "Shutting down MountService");

        // Set initial variables and time out time.
        mActionDone = false;
        final long endShutTime = SystemClock.elapsedRealtime() + MAX_SHUTDOWN_WAIT_TIME;
        synchronized (mActionDoneSync) {
            try {
                final IMountService mount = IMountService.Stub.asInterface(
                        ServiceManager.checkService("mount"));
                if (mount != null) {
                    mount.shutdown(observer);
                } else {
                    Log.w(TAG, "MountService unavailable for shutdown");
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception during MountService shutdown", e);
            }
            while (!mActionDone) {
                long delay = endShutTime - SystemClock.elapsedRealtime();
                if (delay <= 0) {
                    Log.w(TAG, "Shutdown wait timed out");
                    break;
                }
                try {
                    mActionDoneSync.wait(delay);
                } catch (InterruptedException e) {
                }
            }
        }
	} else {
            Log.i( TAG, "Quickboot bypass shutdown MountService");
	}

	if( mShutdownFlow == NORMAL_SHUTDOWN_FLOW ) {
        	rebootOrShutdown(mReboot, mRebootReason);
	} else {
		QuitBootFlowshutDown();
	}
    }

    private static String getCurrentIME(Context context)
  {
    String activeIME = null;
    String ime = Settings.Secure.getString(context.getContentResolver(), "default_input_method");

    if (ime != null)
    {
      activeIME = ime.substring(0, ime.indexOf("/"));
    }
    return activeIME;
  }

    private static ArrayList<String> getAccessibilityServices(Context context)
  {
    if (Settings.Secure.getInt(context.getContentResolver(), "accessibility_enabled", 0) == 0)
    {
      Slog.i("ShutdownManager", "accessibility is disabled");
      return null;
    }

    String servicesValue = Settings.Secure.getString(context.getContentResolver(), "enabled_accessibility_services");

    if ((servicesValue == null) || (servicesValue.equals(""))) {
      Slog.i("ShutdownManager", "no accessibility services exist");
      return null;
    }

    TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
    splitter.setString(servicesValue);
    ArrayList services = new ArrayList();
    while (splitter.hasNext()) {
      String str = splitter.next();
      if ((str != null) && (str.length() > 0))
      {
        ComponentName cn = ComponentName.unflattenFromString(str);
        services.add(cn.getPackageName());
        Log.v("ShutdownManager", "AccessibilityService Package Name = " + cn.getPackageName());
      }
    }
    return services;
  }

    public static void forceStopKillPackages(Context context)
  {
    String WpProcessName;
    int uid;
    String currentIME;
    ArrayList accessibilityServices;

    if(mShutdownWhiteList.isEmpty()) {
     for (int i = 0; i < mHardCodeShutdownList.length; i++) {
      mShutdownWhiteList.add(mHardCodeShutdownList[i]);
    }
    }


    IActivityManager am = ActivityManagerNative.asInterface(ServiceManager.checkService("activity"));

    IPackageManager pm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));

    IWallpaperManager wm = IWallpaperManager.Stub.asInterface(ServiceManager.getService("wallpaper"));

    if ((pm != null) && (am != null) && (wm != null)) {
      try {
        WallpaperInfo wpInfo = wm.getWallpaperInfo();
        String WpPackageName = wpInfo != null ? wpInfo.getPackageName() : null;
        WpProcessName = wpInfo != null ? wpInfo.getServiceInfo().processName : null;
        uid = pm.getPackageUid(WpPackageName, 1000);
        Slog.v("ShutdownManager", "Current Wallpaper = " + WpPackageName + "(" + WpProcessName + ")" + ", uid = " + uid);

        currentIME = getCurrentIME(context);
        Slog.v("ShutdownManager", "Current IME: " + currentIME);

        List<ActivityManager.RunningServiceInfo> sList = am.getServices(30, 0);
        for(ActivityManager.RunningServiceInfo s : sList) {
          if (0L != s.restarting)
          {
            if ((!mShutdownWhiteList.contains(s.service.getPackageName())) && (!s.service.getPackageName().equals(WpPackageName)) && (!s.service.getPackageName().equals(currentIME)) && (!s.service.getPackageName().contains(currentIME)))
            {
              Slog.v("ShutdownManager", "force stop the scheduling service:" + s.service.getPackageName());
              am.forceStopPackage(s.service.getPackageName(), 0);
            }
          }
        }

        List<RunningAppProcessInfo> runningList = am.getRunningAppProcesses();
        accessibilityServices = getAccessibilityServices(context);

        ArrayList homeProcessList = new ArrayList();

        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        List<ResolveInfo> queryHomeList = pm.queryIntentActivities(intent, null, 0, 0);

        if (queryHomeList.size() > 0)
          for (ResolveInfo rinfo : queryHomeList) {
            ComponentInfo ci = rinfo.activityInfo != null ? rinfo.activityInfo : rinfo.serviceInfo;
            if (ci.processName != null) {
              Slog.i("ShutdownManager", "home process: " + ci.processName);
              for (RunningAppProcessInfo p : runningList)
                if (p.processName.equals(ci.processName)) {
                  Slog.i("ShutdownManager", "found running home process shown in above log");
                  runningList.remove(p);
                  runningList.add(runningList.size(), p);
                  homeProcessList.add(p.processName);
                  break;
                }
            }
            else
            {
              Slog.i("ShutdownManager", "query home process name fail!");
            }
          }
        else {
          Slog.i("ShutdownManager", "query home activity fail!");
        }
        for (ActivityManager.RunningAppProcessInfo p : runningList) {
          boolean needForce = true;
          boolean needKill = false;

          if ( homeProcessList.contains(p.processName) || (mShutdownWhiteList.contains(p.processName)) || (p.processName.equals(WpProcessName)) || (p.processName.contains(currentIME)) || ((p.processName.equals("com.google.android.apps.genie.geniewidget")) && (WpProcessName != null) && (WpProcessName.equals("com.google.android.apps.maps:MapsWallpaper"))))
          {
            needForce = false;

            if (p.processName.contains(currentIME) || homeProcessList.contains(p.processName) ) {
              needKill = true;
            }

          }
          else if (p.uid == 1000)
          {
            Slog.v("ShutdownManager", "process = " + p.processName);
          } else if (p.uid == uid) {
            if (!p.processName.equals(WpProcessName)) {
              Slog.i("ShutdownManager", "wallpaper related process = " + p.processName);
              needForce = false;
              needKill = true;
            }
          } else {
            String[] list = pm.getPackagesForUid(p.uid);
            int length = list == null ? 0 : list.length;
            for (int i = 0; i < length; i++) {
              if (mShutdownWhiteList.contains(list[i])) {
                Slog.v("ShutdownManager", "uid-process = " + p.processName + ", whitelist item = " + list[i]);
                needForce = false;
                break;
              }
            }
          }

          if (needForce) {
            for (int i = 0; i < p.pkgList.length; i++) {
              if ((accessibilityServices != null) && (accessibilityServices.contains(p.pkgList[i]))) {
                Slog.i("ShutdownManager", "skip accessibility service: " + p.pkgList[i]);
              } else {
                Slog.i("ShutdownManager", "forceStopPackage: " + p.processName);
                am.forceStopPackage(p.pkgList[i], 0);
              }
            }
          }
          if (needKill)
          {
            Slog.i("ShutdownManager", "killProcess: " + p.processName);
            Process.killProcess(p.pid);
          }
        } 
      }
      catch (RemoteException e)
      {
        Slog.e("ShutdownManager", "RemoteException: " + e);
      }
    }
  }

    private static void QuitBootFlowshutDown() {
       Log.i( TAG, "enter QuitBootFlowshutDown");
       if (SHUTDOWN_VIBRATE_MS > 0) {
           // vibrate before shutting down
           Vibrator vibrator = new SystemVibrator();
           try {
               vibrator.vibrate(SHUTDOWN_VIBRATE_MS);
           } catch (Exception e) {
               // Failure to vibrate shouldn't interrupt shutdown.  Just log it.
               Log.w(TAG, "Failed to vibrate during shutdown.", e);
           }

           // vibrator is asynchronous so we need to wait to avoid shutting down too soon.
           try {
               Thread.sleep(SHUTDOWN_VIBRATE_MS);
           } catch (InterruptedException unused) {
           }
       }

       Log.i( TAG, "QB before goToSleep");
	sInstance.mPowerManager.goToSleep(SystemClock.uptimeMillis());
       Log.i( TAG, "QB after goToSleep");

       removeAllTask(sInstance.mContext);
       
       Log.i( TAG, "QB before forceStopKillPackages");
    	forceStopKillPackages(sInstance.mContext);
       Log.i( TAG, "QB after forceStopKillPackages");

	SystemProperties.set( "sys.qb_shutdown_mode", "1" );

	if(sInstance.mScreenWakeLock != null && sInstance.mScreenWakeLock.isHeld()) {
		sInstance.mScreenWakeLock.release();
		sInstance.mScreenWakeLock = null;
	}

	if (pd != null) {
       Log.i( TAG, "QB pd.dismiss");
                pd.dismiss();
                pd = null;
            } 

            synchronized (sIsStartedGuard) {
                sIsStarted = false;
            }

            //sInstance.mPowerManager.setBacklightBrightnessOff(false); 
            sInstance.mCpuWakeLock.acquire(2000); 

	    SystemProperties.set("ctl.start", "qbd");

      /* Log.i( TAG, "QB before mShutdownThreadSync.wait()");
            synchronized (mShutdownThreadSync) {
                try {
       Log.i( TAG, "QB mShutdownThreadSync.wait()");
                    mShutdownThreadSync.wait();
                } catch (InterruptedException e) {
                }
            }
       Log.i( TAG, "QB after mShutdownThreadSync.wait()");*/
    }

    private void shutdownRadios(int timeout) {
        // If a radio is wedged, disabling it may hang so we do this work in another thread,
        // just in case.
        final long endTime = SystemClock.elapsedRealtime() + timeout;
        final boolean[] done = new boolean[1];
        Thread t = new Thread() {
            public void run() {
                boolean nfcOff;
                boolean bluetoothOff;
                boolean radioOff;

                final INfcAdapter nfc =
                        INfcAdapter.Stub.asInterface(ServiceManager.checkService("nfc"));
                final ITelephony phone =
                        ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
                final IBluetoothManager bluetooth =
                        IBluetoothManager.Stub.asInterface(ServiceManager.checkService(
                                BluetoothAdapter.BLUETOOTH_MANAGER_SERVICE));

                try {
                    nfcOff = nfc == null ||
                             nfc.getState() == NfcAdapter.STATE_OFF;
                    if (!nfcOff) {
                        Log.w(TAG, "Turning off NFC...");
                        nfc.disable(false); // Don't persist new state
                    }
                } catch (RemoteException ex) {
                Log.e(TAG, "RemoteException during NFC shutdown", ex);
                    nfcOff = true;
                }

                try {
                    bluetoothOff = bluetooth == null || !bluetooth.isEnabled();
                    if (!bluetoothOff) {
                        Log.w(TAG, "Disabling Bluetooth...");
                        bluetooth.disable(false);  // disable but don't persist new state
                    }
                } catch (RemoteException ex) {
                    Log.e(TAG, "RemoteException during bluetooth shutdown", ex);
                    bluetoothOff = true;
                }

                try {
                    radioOff = phone == null || !phone.isRadioOn();
                    if (!radioOff) {
                        Log.w(TAG, "Turning off radio...");
                        phone.setRadio(false);
                    }

					//workround for when phone unavailable, the phone state is not off
					radioOff = true;
                } catch (RemoteException ex) {
                    Log.e(TAG, "RemoteException during radio shutdown", ex);
                    radioOff = true;
                }

                Log.i(TAG, "Waiting for NFC, Bluetooth and Radio...");

                while (SystemClock.elapsedRealtime() < endTime) {
                    if (!bluetoothOff) {
                        try {
                            bluetoothOff = !bluetooth.isEnabled();
                        } catch (RemoteException ex) {
                            Log.e(TAG, "RemoteException during bluetooth shutdown", ex);
                            bluetoothOff = true;
                        }
                        if (bluetoothOff) {
                            Log.i(TAG, "Bluetooth turned off.");
                        }
                    }
                    if (!radioOff) {
                        try {
                            radioOff = !phone.isRadioOn();
                        } catch (RemoteException ex) {
                            Log.e(TAG, "RemoteException during radio shutdown", ex);
                            radioOff = true;
                        }
                        if (radioOff) {
                            Log.i(TAG, "Radio turned off.");
                        }
                    }
                    if (!nfcOff) {
                        try {
                            nfcOff = nfc.getState() == NfcAdapter.STATE_OFF;
                        } catch (RemoteException ex) {
                            Log.e(TAG, "RemoteException during NFC shutdown", ex);
                            nfcOff = true;
                        }
                        if (radioOff) {
                            Log.i(TAG, "NFC turned off.");
                        }
                    }

                    if (radioOff && bluetoothOff && nfcOff) {
                        Log.i(TAG, "NFC, Radio and Bluetooth shutdown complete.");
                        done[0] = true;
                        break;
                    }
                    SystemClock.sleep(PHONE_STATE_POLL_SLEEP_MSEC);
                }
            }
        };

        t.start();
        try {
            t.join(timeout);
        } catch (InterruptedException ex) {
        }
        if (!done[0]) {
            Log.w(TAG, "Timed out waiting for NFC, Radio and Bluetooth shutdown.");
        }
    }

    /**
     * Do not call this directly. Use {@link #reboot(Context, String, boolean)}
     * or {@link #shutdown(Context, boolean)} instead.
     *
     * @param reboot true to reboot or false to shutdown
     * @param reason reason for reboot
     */
    public static void rebootOrShutdown(boolean reboot, String reason) {
        if (reboot) {
            Log.i(TAG, "Rebooting, reason: " + reason);
            PowerManagerService.lowLevelReboot(reason);
            Log.e(TAG, "Reboot failed, will attempt shutdown instead");
        } else if (SHUTDOWN_VIBRATE_MS > 0) {
            // vibrate before shutting down
            Vibrator vibrator = new SystemVibrator();
            try {
                vibrator.vibrate(SHUTDOWN_VIBRATE_MS);
            } catch (Exception e) {
                // Failure to vibrate shouldn't interrupt shutdown.  Just log it.
                Log.w(TAG, "Failed to vibrate during shutdown.", e);
            }

            // vibrator is asynchronous so we need to wait to avoid shutting down too soon.
            try {
                Thread.sleep(SHUTDOWN_VIBRATE_MS);
            } catch (InterruptedException unused) {
            }
        }

        // Shutdown power
        Log.i(TAG, "Performing low-level shutdown...");
        PowerManagerService.lowLevelShutdown();
    }

    private static boolean isQuickBoot(Context context) {
	Boolean bQuickBoot = SystemProperties.getBoolean(QB_ENABLE_PROPERTY, true);
	if( bQuickBoot ) {
		bQuickBoot = Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_QUICK_BOOT, 0) != 0;
	}
	return bQuickBoot;
    }

    private static void checkShutdownFlow() {
	mShutdownFlow = ( !mReboot && isQuickBoot(sInstance.mContext) ) ? QB_SHUTDOWN_FLOW : NORMAL_SHUTDOWN_FLOW;
    }

    private static boolean isCurrentHomeActivity(Context context, ComponentName component, ActivityInfo homeInfo) {
        if (homeInfo == null) {
            final PackageManager pm = context.getPackageManager();
            homeInfo = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .resolveActivityInfo(pm, 0);
        }
        return homeInfo != null
            && homeInfo.packageName.equals(component.getPackageName())
            && homeInfo.name.equals(component.getClassName());
    }

    public static void removeAllTask(Context context) {
        final ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            final List<ActivityManager.RecentTaskInfo> recentTasks = am.getRecentTasks(128, ActivityManager.RECENT_IGNORE_UNAVAILABLE);
            int numTasks = recentTasks.size();
            for( int i = 0; i < numTasks; i++ ) {
                final ActivityManager.RecentTaskInfo recentInfo = recentTasks.get(i);
		
		Intent intent = new Intent(recentInfo.baseIntent);
                if (recentInfo.origActivity != null) {
                    intent.setComponent(recentInfo.origActivity);
                }

                // Don't load the current home activity.
                if (isCurrentHomeActivity(context, intent.getComponent(), null)) {
                    continue;
                }

                am.removeTask(recentInfo.persistentId, ActivityManager.REMOVE_TASK_KILL_PROCESS);
            }
        }
    }
}
