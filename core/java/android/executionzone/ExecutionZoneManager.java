package android.executionzone;

/**
 * Created by shahrear on 10/5/15.
 * It will be available in framework through import android.os.ExecutionZoneManager;
 * Use this Singleton class to call the functionality of ExecutionZoneService
 * It is a like a ServiceFetcher for the ContextImpl.
 * created by shah oct 5
 */
import android.util.Log;
import android.os.IBinder;
import android.executionzone.IExecutionZoneService;
import android.os.RemoteException;
import android.util.Log;
import android.content.Context;
import java.util.HashMap;
import java.util.Map;

public class ExecutionZoneManager {
    private static final String TAG = "ExecutionZoneManager";
    private final IExecutionZoneService mExecutionZoneService;
    private static ExecutionZoneManager executionZoneManager;
    private final Context mContext;

    /** Get a handle to the Service.
     * @return the Service, or null.
     */
    public static synchronized ExecutionZoneManager getExecutionZoneManager(Context context){ 
        if(executionZoneManager == null) {
            IBinder binder = android.os.ServiceManager.getService(Context.EXECUTIONZONE_SERVICE);
            if(binder != null) {
                IExecutionZoneService managerService = IExecutionZoneService.Stub.asInterface(binder);
                executionZoneManager = new ExecutionZoneManager(context,managerService);
            } else {
                Log.e(TAG, "Log SHAH ExecutionZoneService binder is null");
            }
        }
        return executionZoneManager;
    }

    /**
     * Use {@link #getExecutionZoneManager} to get the ExecutionZoneManager instance.
     */
    public ExecutionZoneManager(Context ctx, IExecutionZoneService executionZoneService) {
        if(executionZoneService == null){
            throw new IllegalArgumentException("executionzoneservice is null");
        }
        mExecutionZoneService = executionZoneService;
        mContext = ctx;
    }

    /**
     * Sets the value in Service
     * @param zoneName The name of the zone
     * @param policyList Policy list
     */
    public void createZone(String zoneName, String policyList){
        try{
            Log.d(TAG, "Log SHAH Going to call createZone service from ExecutionZoneManager");
            mExecutionZoneService.createZone(zoneName, policyList);
            Log.d(TAG, "Log SHAH Service createZone called successfully from ExecutionZoneManager");
        } catch (Exception e) {
            Log.e(TAG, "Log SHAH FAILED to call createZone service from ExecutionZoneManager, Exception Message: " + e.getMessage());
        }
    }

    /**
     * Sets the value in Service
     * @param packageName The name of the package
     * @param zoneName The zone to be assigned
     */
    public void setZone(String packageName, String zoneName){
        try{
            Log.d(TAG, "Log SHAH Going to call setZone service from ExecutionZoneManager");
            mExecutionZoneService.setZone(packageName, zoneName);
            Log.d(TAG, "Log SHAH Service setZone called successfully from ExecutionZoneManager");
        } catch (Exception e) {
            Log.e(TAG, "Log SHAH FAILED to call setZone service from ExecutionZoneManager, Exception Message: " + e.getMessage());
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
            Log.d(TAG, "Log SHAH Going to call editZone service from ExecutionZoneManager");
            mExecutionZoneService.editZone(zoneName, action, paramList);
            Log.d(TAG, "Log SHAH Service editZone called successfully from ExecutionZoneManager");
        } catch (Exception e) {
            Log.e(TAG, "Log SHAH FAILED to call editZone service from ExecutionZoneManager, Exception Message: " + e.getMessage());
        }
    }

    /**
     * Sets the value in Service
     * @param policyName The name of the policy
     * @param ruleList Policy rule list
     */
    public void createPolicy(String policyName, String ruleList){
        try{
            Log.d(TAG, "Log SHAH Going to call createPolicy service from ExecutionZoneManager");
            mExecutionZoneService.createPolicy(policyName, ruleList);
            Log.d(TAG, "Log SHAH Service createPolicy called successfully from ExecutionZoneManager");
        } catch (Exception e) {
            Log.e(TAG, "Log SHAH FAILED to call createPolicy service from ExecutionZoneManager, Exception Message: " + e.getMessage());
        }
    }

    /**
     * Sets the value in Service
     * @param policyName The name of the policy
     * @param zoneName The name of the zone
     */
    public void setPolicy(String policyName, String zoneName){
        try{
            Log.d(TAG, "Log SHAH Going to call setPolicy service from ExecutionZoneManager");
            mExecutionZoneService.setPolicy(policyName, zoneName);
            Log.d(TAG, "Log SHAH Service setPolicy called successfully from ExecutionZoneManager");
        } catch (Exception e) {
            Log.e(TAG, "Log SHAH FAILED to call setPolicy service from ExecutionZoneManager, Exception Message: " + e.getMessage());
        }
    }

    /**
     * Sets the value in Service
     * @param policyName The name of the policy
     * @param action The action to be performed
     * @param paramList The parameters of the action to be performed
     */
    public void editPolicy(String policyName, String action, String paramList){
        try{
            Log.d(TAG, "Log SHAH Going to call editPolicy service from ExecutionZoneManager");
            mExecutionZoneService.editPolicy(policyName, action, paramList);
            Log.d(TAG, "Log SHAH Service editPolicy called successfully from ExecutionZoneManager");
        } catch (Exception e) {
            Log.e(TAG, "Log SHAH FAILED to call editPolicy service from ExecutionZoneManager, Exception Message: " + e.getMessage());
        }
    }

    /**
     * Sets the value in Service
     * @param permission The name of the permission
     * @param uid User id of the app
     */
    public int checkZonePermission(String permission, int uid){
        int ret = -1111;

        try{
            Log.d(TAG, "Log SHAH Going to call checkZonePermission service from ExecutionZoneManager");
            ret = mExecutionZoneService.checkZonePermission(permission, uid);
            Log.d(TAG, "Log SHAH Service checkZonePermission called successfully from ExecutionZoneManager");
        } catch (Exception e) {
            Log.e(TAG, "Log SHAH FAILED to call checkZonePermission service from ExecutionZoneManager, Exception Message: " + e.getMessage());
        }
        return ret;
    }

    /**
     * Get all zone names
     */
    public String[] getZones()
    {
        String []zones = null;
        try{
            Log.d(TAG, "Log SHAH Going to call getZones service from ExecutionZoneManager");
            zones = mExecutionZoneService.getAllZones();
            Log.d(TAG, "Log SHAH Service getZones called successfully from ExecutionZoneManager");
        } catch (Exception e) {
            Log.e(TAG, "Log SHAH FAILED to call getzones service from ExecutionZoneManager, Exception Message: " + e.getMessage());
        }

        return zones;
    }

    /**
     * Get all policy names
     */
    public String[] getPolicies()
    {
        String []policies = null;
        try{
            Log.d(TAG, "Log SHAH Going to call getPolicies service from ExecutionZoneManager");
            policies = mExecutionZoneService.getAllPolicies();
            Log.d(TAG, "Log SHAH Service getPolicies called successfully from ExecutionZoneManager");
        } catch (Exception e) {
            Log.e(TAG, "Log SHAH FAILED to call getPolicies service from ExecutionZoneManager, Exception Message: " + e.getMessage());
        }

        return policies;
    }

    /**
     * Get rules of a poclicy
     * @param policyname The name of the policy
     */
    public String getRulesOfPolicy(String policyname)
    {
        String ret = null;
        try{
            Log.d(TAG, "Log SHAH Going to call getRulesOfPolicy service from ExecutionZoneManager");
            ret = mExecutionZoneService.getRulesOfPolicy(policyname);
            Log.d(TAG, "Log SHAH Service getRulesOfPolicy called successfully from ExecutionZoneManager");
        } catch (Exception e) {
            Log.e(TAG, "Log SHAH FAILED to call getRulesOfPolicy service from ExecutionZoneManager, Exception Message: " + e.getMessage());
        }
        return ret;
    }

    /**
     * Get zone name of an app
     * @param packagename The name of the package
     */
    public String getZoneOfApp(String packagename)
    {
        String ret = null;
        try{
            Log.d(TAG, "Log SHAH Going to call getZoneOfApp service from ExecutionZoneManager");
            ret = mExecutionZoneService.getZoneOfApp(packagename);
            Log.d(TAG, "Log SHAH Service getZoneOfApp called successfully from ExecutionZoneManager");
        } catch (Exception e) {
            Log.e(TAG, "Log SHAH FAILED to call getZoneOfApp service from ExecutionZoneManager, Exception Message: " + e.getMessage());
        }
        return ret;
    }

    /**
     * Get policynames of a zone
     * @param zonename The name of the zone
     */
    public String[] getPoliciesOfZone(String zonename)
    {
        String[] ret = null;
        try{
            Log.d(TAG, "Log SHAH Going to call getPoliciesOfZone service from ExecutionZoneManager");
            ret = mExecutionZoneService.getPoliciesOfZone(zonename);
            Log.d(TAG, "Log SHAH Service getPoliciesOfZone called successfully from ExecutionZoneManager");
        } catch (Exception e) {
            Log.e(TAG, "Log SHAH FAILED to call getPoliciesOfZone service from ExecutionZoneManager, Exception Message: " + e.getMessage());
        }
        return ret;
    }

    /**
     * Get rules of the policies of a zone
     * @param zonename The name of the zone
     */
    public Map<String,String> getPoliciesOfZoneWithRules(String zonename)
    {
        Map<String,String> ret = null;
        try{
            Log.d(TAG, "Log SHAH Going to call getPoliciesOfZoneWithRules service from ExecutionZoneManager");
            ret = mExecutionZoneService.getPoliciesOfZoneWithRules(zonename);
            Log.d(TAG, "Log SHAH Service getPoliciesOfZoneWithRules called successfully from ExecutionZoneManager");
        } catch (Exception e) {
            Log.e(TAG, "Log SHAH FAILED to call getPoliciesOfZoneWithRules service from ExecutionZoneManager, Exception Message: " + e.getMessage());
        }
        return ret;
    }

}
