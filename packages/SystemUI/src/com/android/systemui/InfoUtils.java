package com.android.systemui;

public class InfoUtils {
    static {
        System.loadLibrary("info_jni");
    }

	public static String GetGPUCurFreq() {
		return native_GetGPUCurFreq();
	}

	public static String[] GetCPUCurFreq() {
		return native_GetCPUCurFreq();
	}

	public static String[] GetTemperature() {
		return native_GetTemperature();
	}

	public static int GetPWMDuty() {
		return native_GetPMWDuty();
	}
	
	//public static String[] GetAVW() {
//		return native_GetAVW();
//	}

	public static void GetCPUUsage(int core[]) {
		native_GetCPUUsage(core);
	}

	public static int OpenINA231() {
		return native_OpenINA231();
	}
	public static void CloseINA231() {
		native_CloseINA231();
	}

	public static void GetINA231(float value[]) {
		native_GetINA231(value);
	}

    private native static String native_GetGPUCurFreq();
    private native static String[] native_GetCPUCurFreq();
    private native static String[] native_GetTemperature();
    private native static int native_GetPMWDuty();
    //private native static String[] native_GetAVW();
    private native static int native_OpenINA231();
    private native static void native_CloseINA231();
    private native static void native_GetINA231(float value[]);
    private native static void native_GetCPUUsage(int core[]);
}
