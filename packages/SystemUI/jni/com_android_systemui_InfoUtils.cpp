//#include <jni.h>
#include <JNIHelp.h>
#include <utils/Log.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/ioctl.h>
#include <linux/ioctl.h>

#define GPUFREQ_NODE "/sys/class/mpgpu/cur_freq"
#define TEMP_NODE "/sys/devices/virtual/thermal/thermal_zone0/temp"

#define LOG_TAG "Info-JNI"

namespace android {

static jstring native_GetGPUCurFreq(JNIEnv* env, jobject obj) {
    FILE *fp = NULL;
    char buf[4] = {'\0',};
    fp = fopen(GPUFREQ_NODE, "r");

    if (fp == NULL)
        return NULL;
    else
        fread(buf, 1, 3, fp);

    fclose(fp);

    return env->NewStringUTF(buf);
}

static jstring native_GetCPUCurFreq(JNIEnv* env, jobject obj) {
    FILE *fp = NULL;
    char buf[8] = {'\0',};

    fp = fopen("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_cur_freq", "r");

    if (fp == NULL)
        return NULL;
    else
        fread(buf, 1, 7, fp);

    //ALOGE("buf[%d] = %s", i, buf[i]);

    if (buf[6] == '\n')
        buf[3] = '\0';
    else
        buf[4] = '\0';

    fclose(fp);

    return env->NewStringUTF(buf);
}

static jstring native_GetTemperature(JNIEnv* env, jobject obj) {
    FILE *fp = NULL;

    fp = fopen(TEMP_NODE, "r");

    if (fp == NULL) {
        ALOGE("Not opened temp");
        return NULL;
    }

    char buf[4] = {'\0',};
    int read = -1;

    read = fread(buf, 1, 4, fp);

    fclose(fp);

    return env->NewStringUTF(buf);
}

int mOldUserCPU[4];
int mOldSystemCPU[4];
int mOldIdleCPU[4];

static int calUsage(int cpu_idx, int user, int nice, int system, int idle) {
    long total = 0;
    long usage = 0;
    int diff_user, diff_system, diff_idle;

    diff_user = mOldUserCPU[cpu_idx] - user;
    diff_system = mOldSystemCPU[cpu_idx] - system;
    diff_idle = mOldIdleCPU[cpu_idx] - idle;

    total = diff_user + diff_system + diff_idle;
    if (total != 0)
        usage = diff_user * 100 / total;

    mOldUserCPU[cpu_idx] = user;
    mOldSystemCPU[cpu_idx] = system;
    mOldIdleCPU[cpu_idx] = idle;
    return usage;
}

static void native_GetCPUUsage(JNIEnv* env, jobject obj, jintArray arr) {
    int freq = 0;
    char buf[80] = {0,};
    char cpuid[8] = "cup";
    int findCPU = 0;
    int user, system, nice, idle;
    FILE *fp;
    int cpu_index = 0;

    fp = fopen("/proc/stat", "r");
    if (fp == NULL)
        return;

    jint *usage = env->GetIntArrayElements(arr, NULL);

    int first = 0;

    while(fgets(buf, 80, fp)) {
        char temp[4] = "cpu";
        temp[3] = '0' + cpu_index;
        if (!strncmp(buf, temp, 4)) {
            findCPU = 1;
            sscanf(buf, "%s %d %d %d %d",
            cpuid, &user, &nice, &system, &idle);
            usage[cpu_index] = calUsage(cpu_index, user, nice, system, idle);
            cpu_index++;
        }
        if (!strncmp(buf, "intr", 4))
            break;
        if (findCPU == 0)
            mOldUserCPU[cpu_index] = mOldSystemCPU[cpu_index] = mOldIdleCPU[cpu_index] = 0;
        else
            findCPU = 0;
    }

    fclose(fp);

    env->ReleaseIntArrayElements(arr, usage, 0);

    return;
}

static const JNINativeMethod g_methods[] = {
    { "native_GetGPUCurFreq", "()Ljava/lang/String;", (jstring*)native_GetGPUCurFreq },
    { "native_GetCPUCurFreq", "()Ljava/lang/String;", (jstring*)native_GetCPUCurFreq },
    { "native_GetTemperature", "()Ljava/lang/String;", (jstring)native_GetTemperature },
    { "native_GetCPUUsage", "([I)V", (void*)native_GetCPUUsage },
};

int register_com_android_systemui(JNIEnv *env) {
    if (jniRegisterNativeMethods(
            env, "com/android/systemui/InfoUtils", g_methods, NELEM(g_methods)) < 0) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}

} // namespace android

int JNI_OnLoad(JavaVM *jvm, void* reserved) {
    JNIEnv *env;

    if (jvm->GetEnv((void**)&env, JNI_VERSION_1_6)) {
        return JNI_ERR;
    }

    return android::register_com_android_systemui(env);
}
