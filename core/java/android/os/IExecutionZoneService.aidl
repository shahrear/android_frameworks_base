/*
 * aidl file :
 * frameworks/base/core/java/android/os/IExecutionZoneService.aidl
 * This file contains definitions of functions which are
 * exposed by service.
 */
package android.os;
/**{@hide}*/
interface IExecutionZoneService {
        void setZone(String packageName, String zoneName);
}