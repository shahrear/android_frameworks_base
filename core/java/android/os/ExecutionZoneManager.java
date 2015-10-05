package android.os;

/**
 * Created by shahrear on 10/5/15.
 * It will be available in framework through import android.os.ExecutionZoneManager;
 * Use this Singleton class to call the functionality of ExecutionZoneService
 * It is a like a ServiceFetcher for the ContextImpl.
 */
import android.os.IBinder;
import android.os.IExecutionZoneService;
import android.os.RemoteException;
import android.util.Log;

public class ExecutionZoneManager {
    private static final String TAG = "ExecutionZoneManager";
    private final IExecutionZoneService mExecutionZoneService;
    private static ExecutionZoneManager executionZoneManager;

    /** Get a handle to the Service.
     * @return the Service, or null.
     */
    public static synchronized ExecutionZoneManager getExecutionZoneManager() {
        if(executionZoneManager == null) {
            IBinder binder = android.os.ServiceManager.getService("executionzone");
            if(binder != null) {
                IExecutionZoneService managerService = IExecutionZoneService.Stub.asInterface(binder);
                executionZoneManager = new ExecutionZoneManager(managerService);
            } else {
                Log.e(TAG, "ExecutionZoneService binder is null");
            }
        }
        return executionZoneManager;
    }

    /**
     * Use {@link #getExecutionZoneManager} to get the ExecutionZoneManager instance.
     */
    ExecutionZoneManager(IExecutionZoneService executionZoneService) {
        if(executionZoneService == null){
            throw new IllegalArgumentException("executionzoneservice is null");
        }
        mExecutionZoneService = executionZoneService;
    }
    /**
     * Sets the value in Service
     * @param arg
     */
    public void setZone(String packageName, String zoneName){
        try{
            Log.d(TAG, "Going to call service from framework proxy");
            mExecutionZoneService.setZone(packageName,zoneName);
            Log.d(TAG, "Service called successfully from framework proxy");
        } catch (Exception e) {
            Log.d(TAG, "FAILED to call service from framework proxy");
            e.printStackTrace();
        }
    }
    /**
     * Get the binder of IExecutionZoneService.
     */
    public IExecutionZoneService getExecutionZoneService(){
        return mExecutionZoneService;
    }

}
