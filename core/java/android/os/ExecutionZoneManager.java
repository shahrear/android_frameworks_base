package android.os;

/**
 * Created by shahrear on 10/5/15.
 * It will be available in framework through import android.os.ExecutionZoneManager;
 * Use this Singleton class to call the functionality of ExecutionZoneService
 * It is a like a ServiceFetcher for the ContextImpl.
 * created by shah oct 5
 */
import android.util.Log;
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
            IBinder binder = android.os.ServiceManager.getService("execution_zone");
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
     * @param zoneName The name of the zone
     * @param policyList Policy list
     */
    public void createZone(String zoneName, String policyList){
        try{
            Log.d(TAG, "Going to call createZone service from ExecutionZoneManager");
            mExecutionZoneService.createZone(zoneName, policyList);
            Log.d(TAG, "Service createZone called successfully from ExecutionZoneManager");
        } catch (Exception e) {
            Log.d(TAG, "FAILED to call createZone service from ExecutionZoneManager, Exception Message: " + e.getMessage());
        }
    }

    /**
     * Sets the value in Service
     * @param packageName The name of the package
     * @param zoneName The zone to be assigned
     */
    public void setZone(String packageName, String zoneName){
        try{
            Log.d(TAG, "Going to call setZone service from ExecutionZoneManager");
            mExecutionZoneService.setZone(packageName, zoneName);
            Log.d(TAG, "Service setZone called successfully from ExecutionZoneManager");
        } catch (Exception e) {
            Log.d(TAG, "FAILED to call setZone service from ExecutionZoneManager, Exception Message: " + e.getMessage());
        }
    }

    /**
     * Sets the value in Service
     * @param zoneName The name of the zone
     * @param action The action to be performed
     * @param paramList The parameters of the action to be performed
     */
    public void editZone(String zoneName, String action, String paramList){
        try{
            Log.d(TAG, "Going to call editZone service from ExecutionZoneManager");
            mExecutionZoneService.editZone(zoneName, action, paramList);
            Log.d(TAG, "Service editZone called successfully from ExecutionZoneManager");
        } catch (Exception e) {
            Log.d(TAG, "FAILED to call editZone service from ExecutionZoneManager, Exception Message: " + e.getMessage());
        }
    }

    /**
     * Sets the value in Service
     * @param policyName The name of the policy
     * @param ruleList Policy rule list
     */
    public void createPolicy(String policyName, String ruleList){
        try{
            Log.d(TAG, "Going to call createPolicy service from ExecutionZoneManager");
            mExecutionZoneService.createPolicy(policyName, ruleList);
            Log.d(TAG, "Service createPolicy called successfully from ExecutionZoneManager");
        } catch (Exception e) {
            Log.d(TAG, "FAILED to call createPolicy service from ExecutionZoneManager, Exception Message: " + e.getMessage());
        }
    }

    /**
     * Sets the value in Service
     * @param policyName The name of the policy
     * @param zoneName The name of the zone
     */
    public void setPolicy(String policyName, String zoneName){
        try{
            Log.d(TAG, "Going to call setPolicy service from ExecutionZoneManager");
            mExecutionZoneService.setPolicy(policyName, zoneName);
            Log.d(TAG, "Service setPolicy called successfully from ExecutionZoneManager");
        } catch (Exception e) {
            Log.d(TAG, "FAILED to call setPolicy service from ExecutionZoneManager, Exception Message: " + e.getMessage());
        }
    }

    /**
     * Sets the value in Service
     * @param policyName The name of the policy
     * @param action The action to be performed
     * @param paramList The parameters of the action to be performed
     */
    public
    {
        try{
            Log.d(TAG, "Going to call editPolicy service from ExecutionZoneManager");
            mExecutionZoneService.editPolicy(policyName, action, paramList);
            Log.d(TAG, "Service editPolicy called successfully from ExecutionZoneManager");
        } catch (Exception e) {
            Log.d(TAG, "FAILED to call editPolicy service from ExecutionZoneManager, Exception Message: " + e.getMessage());
        }
    }

}
