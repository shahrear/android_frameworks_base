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

#define TEMP_NODE "/sys/devices/platform/tmu/temperature"

#define LOG_TAG "Info_JNI"

namespace android {

#define CPU_FREQ_NOED "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq"

static jstring native_GetCPUCurFreq(JNIEnv* env, jobject obj) {
	FILE *fp = NULL;
	char buf[8] = {'\0',};

	fp = fopen(CPU_FREQ_NOED, "r");

	if (fp == NULL) {
		ALOGE("Not opened CPU freq");
		buf[0] = '0';
		buf[1] = '\0';
		memset(buf, '\0', 8);
		return env->NewStringUTF(buf);
	}

	fread(buf, 1, 7, fp);

	if (buf[6] == '\n')
		buf[3] = '\0';
	else
		buf[4] = '\0';

	fclose(fp);

	return env->NewStringUTF(buf);
}

static jstring native_GetTemperature(JNIEnv* env, jobject obj) {
	FILE *fp = NULL;
	char buf[13] = {'\0',};

	fp = fopen(TEMP_NODE, "r");

	if (fp == NULL) {
		ALOGE("Not opened temp");
		return NULL;
	}

	fread(buf, 1, 12, fp);

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
    { "native_GetCPUCurFreq", "()Ljava/lang/String;", (jstring*)native_GetCPUCurFreq },
    { "native_GetTemperature", "()Ljava/lang/String;", (jstring*)native_GetTemperature },
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
