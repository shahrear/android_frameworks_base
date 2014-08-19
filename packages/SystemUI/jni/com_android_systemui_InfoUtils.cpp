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

#define GPUFREQ_NODE "/sys/devices/11800000.mali/clock"
#define TEMP_NODE "/sys/devices/10060000.tmu/temp"
#define PWM_DUTY_NODE "/sys/devices/platform/s3c24xx-pwm.0/odroidxu-fan/pwm_duty"

#define LOG_TAG "Info-JNI"

namespace android {

static jstring native_GetGPUCurFreq(JNIEnv* env, jobject obj) {
	FILE *fp = NULL;
	char buf[4] = {'\0',};
	fp = fopen(GPUFREQ_NODE, "r");

	if (fp == NULL) {
		return 0;
	}

	fread(buf, 1, 3, fp);

	fclose(fp);

	return env->NewStringUTF(buf);
}

char *cpu_node_list[64] = {
		"/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_cur_freq",
		"/sys/devices/system/cpu/cpu1/cpufreq/cpuinfo_cur_freq",
		"/sys/devices/system/cpu/cpu2/cpufreq/cpuinfo_cur_freq",
		"/sys/devices/system/cpu/cpu3/cpufreq/cpuinfo_cur_freq",
		"/sys/devices/system/cpu/cpu4/cpufreq/cpuinfo_cur_freq",
		"/sys/devices/system/cpu/cpu5/cpufreq/cpuinfo_cur_freq",
		"/sys/devices/system/cpu/cpu6/cpufreq/cpuinfo_cur_freq",
		"/sys/devices/system/cpu/cpu7/cpufreq/cpuinfo_cur_freq", };

static jobjectArray native_GetCPUCurFreq(JNIEnv* env, jobject obj) {
	FILE *fp = NULL;
	char buf[8][8] = {{'\0',},};
	jobjectArray ret = (jobjectArray)env->NewObjectArray(8, env->FindClass("java/lang/String"), 
			env->NewStringUTF(""));

	for (int i = 0; i < 8; i++) {
		fp = fopen(cpu_node_list[i], "r");

		if (fp == NULL) {
			ALOGE("Not opened CPU freq");
			buf[i][0] = '0';
			buf[i][1] = '\0';
			env->SetObjectArrayElement(ret, i, env->NewStringUTF(buf[i]));
			memset(buf, '\0', 8);
			continue;
		}

		fread(&buf[i], 1, 7, fp);

		//ALOGE("buf[%d] = %s", i, buf[i]);

		if (buf[i][6] == '\n')
			buf[i][3] = '\0';
		else
			buf[i][4] = '\0';

		fclose(fp);

		env->SetObjectArrayElement(ret, i, env->NewStringUTF(buf[i]));

		memset(buf, '\0', 8);
	}

	return ret;
}

static jobjectArray native_GetTemperature(JNIEnv* env, jobject obj) {
	FILE *fp = NULL;

	fp = fopen(TEMP_NODE, "r");

	if (fp == NULL) {
		ALOGE("Not opened temp");
		return NULL;
	}

    char buf[24] = {'\0',};
    int read = -1;

	jobjectArray ret = (jobjectArray)env->NewObjectArray(5, env->FindClass("java/lang/String"), 
			env->NewStringUTF(""));

	char value[5][3] = {{'\0',},};

    int idx = 0;
    do {
	    read = fread(buf, 1, 16, fp);
        if (read > 0) { 
            strncpy(value[idx], &buf[10], 2);
	        env->SetObjectArrayElement(ret, idx, env->NewStringUTF(value[idx]));
            idx++;
            //memset(buf, '\0', 16);
        }
    } while (read > 0);

	fclose(fp);

	return ret;
}

static int native_GetPMWDuty(JNIEnv* env, jobject obj) {
	FILE *fp = NULL;
	char buf[4] = {'\0',};
	fp = fopen(PWM_DUTY_NODE, "r");

	if (fp == NULL) {
		ALOGE("not opened pwm duty");
		return 0;
	}

	fread(buf, 1, 4, fp);

	fclose(fp);

	int value = atoi(buf);

	return (int)((double)(100.0 / 255.0) * value);
}

// 
//  INA231(Sensor) Interface Example
//  2013.07.17
//

// Kernel/drivers/hardkernel/ina231-misc.h
typedef struct ina231_iocreg__t {
    unsigned char   name[20];
    unsigned int    enable;
	unsigned int	cur_uV;
	unsigned int	cur_uA;
	unsigned int	cur_uW;
}   ina231_iocreg_t;

#define INA231_IOCGREG		_IOR('i', 1, ina231_iocreg_t *)
#define INA231_IOCSSTATUS	_IOW('i', 2, ina231_iocreg_t *)
#define INA231_IOCGSTATUS   _IOR('i', 3, ina231_iocreg_t *)

#define DEV_SENSOR_ARM  "/dev/sensor_arm"
#define DEV_SENSOR_MEM  "/dev/sensor_mem"
#define DEV_SENSOR_KFC  "/dev/sensor_kfc"
#define DEV_SENSOR_G3D  "/dev/sensor_g3d"

enum    {
    SENSOR_ARM = 0,
    SENSOR_MEM,
    SENSOR_KFC,
    SENSOR_G3D,
    SENSOR_MAX
};

typedef struct  sensor__t  {
    int             fd;
    ina231_iocreg_t data;      
}   sensor_t;

int open_sensor(const char *node, sensor_t *sensor)
{
    if((sensor->fd = open(node, O_RDWR)) < 0)   
		ALOGE("%s Open Fail!\n", node);
    else                                        
		ALOGE("%s Open Success!\n", node);

	return sensor->fd;
}

void enable_sensor(sensor_t *sensor, unsigned char enable)
{
    if(sensor->fd > 0)  {
        sensor->data.enable = enable ? 1 : 0;
        if(ioctl(sensor->fd, INA231_IOCSSTATUS, &sensor->data) < 0)    
            ALOGE("%s IOCTL Error! %d\n", sensor->data.name, __LINE__);
        else    
            ALOGE("%s %s!\n", sensor->data.name, enable ? "enable" : "disable");
    }
}

int read_sensor_status(sensor_t *sensor)
{
    if(sensor->fd > 0)  {
        if(ioctl(sensor->fd, INA231_IOCGSTATUS, &sensor->data) < 0) {
            ALOGE("%s IOCTL Error! %d\n", sensor->data.name, __LINE__);
			return -1;
		} else
            ALOGE("%s read status!\n", sensor->data.name);
    }
	return 0;
}

void read_sensor(sensor_t *sensor)
{
    if(sensor->fd > 0)  {
        if(ioctl(sensor->fd, INA231_IOCGREG, &sensor->data) < 0)  
            ALOGE("%s IOCTL Error! %d\n", sensor->data.name, __LINE__);
		/*
        else    {
            ALOGE("Name : %s,  current uV : %d, current uA = %d, current uW = %d\n",
                sensor->data.name, sensor->data.cur_uV, sensor->data.cur_uA, sensor->data.cur_uW);
        }
		*/
    }
}

void close_sensor(sensor_t *sensor)
{
    if(sensor->fd > 0)  close(sensor->fd);
}

sensor_t    sensor[SENSOR_MAX];

static int native_OpenINA231(JNIEnv* env, jobject obj) {
	// Sensor Device Open
    if (open_sensor(DEV_SENSOR_ARM, &sensor[SENSOR_ARM]) < 0)
		return -1;
	if (open_sensor(DEV_SENSOR_MEM, &sensor[SENSOR_MEM]) < 0)
		return -1;
    if (open_sensor(DEV_SENSOR_KFC, &sensor[SENSOR_KFC]) < 0)
		return -1;
	if (open_sensor(DEV_SENSOR_G3D, &sensor[SENSOR_G3D]) < 0)
		return -1; 

	// Sensor Status Check
    if (read_sensor_status(&sensor[SENSOR_ARM]))
		return -1;
	if (read_sensor_status(&sensor[SENSOR_MEM]))
		return -1;
    if (read_sensor_status(&sensor[SENSOR_KFC]))
		return -1;
	if (read_sensor_status(&sensor[SENSOR_G3D]))
		return -1;
   
	// Sensor Enable
    if(!sensor[SENSOR_ARM].data.enable)     
		enable_sensor(&sensor[SENSOR_ARM], 1);
    if(!sensor[SENSOR_MEM].data.enable)     
		enable_sensor(&sensor[SENSOR_MEM], 1);
    if(!sensor[SENSOR_KFC].data.enable)     
		enable_sensor(&sensor[SENSOR_KFC], 1);
    if(!sensor[SENSOR_G3D].data.enable)     
		enable_sensor(&sensor[SENSOR_G3D], 1);
    
	return 0;
}

static void native_CloseINA231(JNIEnv* env, jobject obj) {
	
	// Sensor Disable
    if(sensor[SENSOR_ARM].data.enable)      
		enable_sensor(&sensor[SENSOR_ARM], 0);
    if(sensor[SENSOR_MEM].data.enable)      
		enable_sensor(&sensor[SENSOR_MEM], 0);
    if(sensor[SENSOR_KFC].data.enable)      
		enable_sensor(&sensor[SENSOR_KFC], 0);
    if(sensor[SENSOR_G3D].data.enable)      
		enable_sensor(&sensor[SENSOR_G3D], 0);

    // Sensor Device Close
    close_sensor(&sensor[SENSOR_ARM]);    
	close_sensor(&sensor[SENSOR_MEM]);
    close_sensor(&sensor[SENSOR_KFC]);    
	close_sensor(&sensor[SENSOR_G3D]);
}

static void native_GetINA231(JNIEnv* env, jobject obj, jfloatArray arr) {
       
	jfloat *avw = env->GetFloatArrayElements(arr, NULL);
    
    // Sensor Data Display
	read_sensor(&sensor[SENSOR_ARM]);   
	read_sensor(&sensor[SENSOR_MEM]);
	read_sensor(&sensor[SENSOR_KFC]);   
	read_sensor(&sensor[SENSOR_G3D]);

	avw[0] = (float)(sensor[SENSOR_ARM].data.cur_uV / 100000) / 10;
	avw[1] = (float)(sensor[SENSOR_ARM].data.cur_uA / 1000) / 1000;
	avw[2] = (float)(sensor[SENSOR_ARM].data.cur_uW / 1000) / 1000;
	avw[3] = (float)(sensor[SENSOR_KFC].data.cur_uV / 100000) / 10;
	avw[4] = (float)(sensor[SENSOR_KFC].data.cur_uA / 1000) / 1000;
	avw[5] = (float)(sensor[SENSOR_KFC].data.cur_uW / 1000) / 1000;
	avw[6] = (float)(sensor[SENSOR_G3D].data.cur_uV / 100000) / 10;
	avw[7] = (float)(sensor[SENSOR_G3D].data.cur_uA / 1000) / 1000;
	avw[8] = (float)(sensor[SENSOR_G3D].data.cur_uW / 1000) / 1000;
	avw[9] = (float)(sensor[SENSOR_MEM].data.cur_uV / 100000) / 10;
	avw[10] = (float)(sensor[SENSOR_MEM].data.cur_uA / 1000) / 1000;
	avw[11] = (float)(sensor[SENSOR_MEM].data.cur_uW / 1000) / 1000;

	env->ReleaseFloatArrayElements(arr, avw, 0);
}

int mOldUserCPU[8];
int mOldSystemCPU[8];
int mOldIdleCPU[8];

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
    { "native_GetCPUCurFreq", "()[Ljava/lang/String;", (jobjectArray*)native_GetCPUCurFreq },
    { "native_GetTemperature", "()[Ljava/lang/String;", (jobjectArray*)native_GetTemperature },
    { "native_GetPMWDuty", "()I", (void*)native_GetPMWDuty },
    //{ "native_GetAVW", "()[Ljava/lang/String;", (jobjectArray*)native_GetAVW },
    { "native_OpenINA231", "()I", (int*)native_OpenINA231 },
    { "native_CloseINA231", "()V", (void*)native_CloseINA231 },
    { "native_GetINA231", "([F)V", (void*)native_GetINA231 },
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
