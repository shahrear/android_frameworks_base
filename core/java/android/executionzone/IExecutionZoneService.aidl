/*
 * aidl file :
 * frameworks/base/core/java/android/os/IExecutionZoneService.aidl
 * This file contains definitions of functions which are
 * exposed by service.
 * created by shah oct 5
 */
package android.executionzone;

import java.util.Map;

interface IExecutionZoneService {
        boolean createZone(String zoneName, String policyList);
        boolean setZone(String packageName, String zoneName);
        boolean editZone(String zoneName, String action, String paramList);
        boolean createPolicy(String policyName, String ruleList);
        boolean setPolicy(String policyName, String zoneName);
        boolean editPolicy(String policyName, String action, String paramList);
        int checkZonePermission(String permission, int uid);
        String[] getAllZones();
        String[] getAllPolicies();
        String getRulesOfPolicy(String policname);
        String getZoneOfApp(String packagename);
        String[] getPoliciesOfZone(String zonename);
        Map getPoliciesOfZoneWithRules(String zonename);
}
