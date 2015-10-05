package com.android.server;

import android.os.Handler;
import android.os.IExecutionZoneService;
import android.os.Looper;
import android.os.Message;
import android.content.Context;
import android.os.Process;
import android.util.Log;


/**
 * Created by shahrear on 10/5/15.
 */
public class ExecutionZoneService extends IExecutionZoneSerivce.Stub {
    private static final String TAG = "ExecutionZoneService";
    private ExecutionZoneWorkerThread mWorker;
    private ExecutionZoneWorkerHandler mHandler;
    private Context mContext;
    public ExecutionZoneService(Context context) {
        super();
        mContext = context;
        mWorker = new ExecutionZoneWorkerThread("ExecutionZoneServiceWorker");
        mWorker.start();
        Log.i(TAG, "Spawned worker thread");
    }

    public void setZone(String packageName, String zoneName) {
        Log.i(TAG, "setZone " + packageName + "to zone " + zoneName);
        Message msg = Message.obtain();
        msg.what = ExecutionZoneWorkerHandler.MESSAGE_SET;
        msg.arg1 = val;
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

    private class ExecutionZoneWorkerHandler extends Handler {
        private static final int MESSAGE_SET = 0;
        @Override
        public void handleMessage(Message msg) {
            try {
                if (msg.what == MESSAGE_SET) {
                    Log.i(TAG,"set message received:"+msg.arg1);
                }
            } catch (Exception e) {
                // Log, don't crash!
                Log.e(TAG, "Exception in handleMessage");
            }
        }
    }

}
