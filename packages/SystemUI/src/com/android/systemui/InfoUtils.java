package com.android.systemui;

public class InfoUtils {
    static {
        System.loadLibrary("info_jni");
    }

    public static String GetGPUCurFreq() {
        return native_GetGPUCurFreq();
    }

    public static String GetCPUCurFreq() {
        return native_GetCPUCurFreq();
    }

    public static String GetTemperature() {
        return native_GetTemperature();
    }

    public static void GetCPUUsage(int core[]) {
        native_GetCPUUsage(core);
    }

    private native static String native_GetGPUCurFreq();
    private native static String native_GetCPUCurFreq();
    private native static String native_GetTemperature();
    private native static void native_GetCPUUsage(int core[]);
}
