#include <linux/input.h>
#include <stdio.h>
#include <fcntl.h>
#include <sys/poll.h>
#include <pthread.h>
#include <hardware_legacy/power.h>

#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <fcntl.h>
#include <dirent.h>
#include <unistd.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <linux/netlink.h>
//#include <private/android_filesystem_config.h>
#include <sys/time.h>
#include <asm/page.h>

#include <cutils/properties.h>
#include <binder/IPCThreadState.h>
#include <binder/ProcessState.h>
#include <binder/IServiceManager.h>
#include <utils/Log.h>
#include <utils/threads.h>
#if defined(HAVE_PTHREADS)
# include <pthread.h>
# include <sys/resource.h>
#endif
#include "BootAnimation.h"
#include<semaphore.h>
#include "unpackimg.h"
#include <stdio.h>

using namespace android;

#define SHOW_DEBUG_LOG 1

#define PWRKEY_UP_VALUE 0
#define PWRKEY_DOWN_VALUE 1

#define ANDROID_ANIM_TIME 3
#define CHARGE_ANIM_TIME 3

#define PWRKEY_LONGPRESS_TIME 1

#define POWER_SUPPLY_SUBSYSTEM "power_supply"
#define POWER_SUPPLY_SYSFS_PATH "/sys/class/" POWER_SUPPLY_SUBSYSTEM
#define AC_CHARGE_PATH "/sys/class/power_supply/ac/online"

#define TYPE_QB_BOOT_ANIM 0
#define TYPE_QB_CHARGE_ANIM 1
#define TYPE_QB_CHARGE_FULL 2

struct uevent {
    const char *action;
    const char *path;
    const char *subsystem;
    const char *firmware;
    int major;
    int minor;
    int state;
    const char *usb_state;
    const char *power_supply_state;
};

static int g_powerkey_has_down = 0;
static pthread_mutex_t g_bootup_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_mutex_t g_drawlogo_mutex = PTHREAD_MUTEX_INITIALIZER;
static int g_has_boot_up = 0;
static int g_charge = 0;
static int g_chargeanim_showed = 0;
static sp<BootAnimation> g_boot_anim = NULL;
static pthread_t s_tid_drawlogo_manager;
static sem_t draw_sem;
static int draw_mode = 0;

/*
 * gettime() - returns the time in seconds of the system's monotonic clock or
 * zero on error.
 */
time_t gettime(void)
{
    struct timespec ts;
    int ret;

    ret = clock_gettime(CLOCK_MONOTONIC, &ts);
    if (ret < 0) {
        ALOGI("quick_boot clock_gettime(CLOCK_MONOTONIC) failed: %s\n", strerror(errno));
        return -1;
    }

    return ts.tv_sec;
}

int readFromFile(const char* path, char* buf, size_t size) {
    char *cp = NULL;

    int fd = open(path, O_RDONLY, 0);
    if (fd == -1) {
        printf("Could not open '%s'\n", path);
        return -1;
    }

    ssize_t count = TEMP_FAILURE_RETRY(read(fd, buf, size));
    if (count > 0)
            cp = (char *)memrchr(buf, '\n', count);

    if (cp)
        *cp = '\0';
    else
        buf[0] = '\0';

    close(fd);
    return count;
}

void show_anim(int nType) {
	pthread_mutex_lock(&g_drawlogo_mutex);
	if(g_boot_anim != NULL) {
		g_boot_anim->request_exit();
	}

	if(nType == TYPE_QB_BOOT_ANIM) {
		g_boot_anim = new BootAnimation();
		g_boot_anim->showQBBootLogo(true);
	} else if( (nType == TYPE_QB_CHARGE_ANIM) && (g_has_boot_up != 1) ) {
		g_boot_anim = new BootAnimation();
		g_boot_anim->showQBChargeAnim(true);
	} else if( (nType == TYPE_QB_CHARGE_FULL) && (g_has_boot_up != 1) ) {
		g_boot_anim = new BootAnimation();
                g_boot_anim->showQBChargeFull(true);
	}
	pthread_mutex_unlock(&g_drawlogo_mutex);
}

void startAndroidAnim() {
#if SHOW_DEBUG_LOG
	ALOGI("quick_boot startAndroidAnim\n");
#endif
	draw_mode = TYPE_QB_BOOT_ANIM;
        sem_post(&draw_sem);
}

void sendQBTurnOnScreenIntent() {
#if SHOW_DEBUG_LOG
        ALOGI("quick_boot sendQBTurnOnScreenIntent\n");
#endif

        char* cmd = NULL;
        asprintf(&cmd, "/system/bin/am broadcast -a android.intent.action.ACTION_TURNON_SCREEN_QB");
        if( cmd != NULL ) {
                system(cmd);
                free(cmd);
                fprintf(stderr, "quickboot sendQBTurnOnScreenIntent success!\n" );
        }
}

void sendQBTurnOffScreenIntent() {
#if SHOW_DEBUG_LOG
        ALOGI("quick_boot sendQBTurnOffScreenIntent\n");
#endif
	
	char* cmd = NULL;
        asprintf(&cmd, "/system/bin/am broadcast -a android.intent.action.ACTION_TURNOFF_SCREEN_QB");
        if( cmd != NULL ) {
                system(cmd);
                free(cmd);
                fprintf(stderr, "quickboot sendQBTurnOffScreenIntent success!\n" );
        }
}

void sendQBBootIntent() {
	char* cmd = NULL;
        asprintf(&cmd, "/system/bin/am broadcast -a android.intent.action.ACTION_BOOT_QB");
        if( cmd != NULL ) {
                system(cmd);
                free(cmd);
		fprintf(stderr, "quickboot sendQBBootIntent success!\n" );
        }
}

void sendPwrkey() {
	char* cmd = NULL;
        asprintf(&cmd, "/system/bin/input keyevent 26");
        if( cmd != NULL ) {
                system(cmd);
                free(cmd);
                fprintf(stderr, "quickboot sendPwrkey success!\n" );
        }
}

void enterGpioKeyQuickbootMode() {
	int fd = -1 ;
        fd = open("/sys/bus/platform/drivers/gpio-key/quick_boot_mode", O_WRONLY);
        if( fd >= 0 ) {
                int w_num = -1 ;
                w_num = write(fd, "1", strlen("1"));
                if( w_num < 0 ) {
                        fprintf(stderr, "write /sys/bus/platform/drivers/gpio-key/quick_boot_mode fail!\n" );
                }
                close(fd);
        } else {
                fprintf(stderr, "open /sys/bus/platform/drivers/gpio-key/quick_boot_mode fail!\n" );
        }
}

void exitGpioKeyQuickbootMode() {
	int fd = -1 ;
	fd = open("/sys/bus/platform/drivers/gpio-key/quick_boot_mode", O_WRONLY);
	if( fd >= 0 ) {
		int w_num = -1 ;
		w_num = write(fd, "0", strlen("0"));
		if( w_num < 0 ) {
			fprintf(stderr, "write /sys/bus/platform/drivers/gpio-key/quick_boot_mode fail!\n" );
		}
		close(fd);	
	} else {
		fprintf(stderr, "open /sys/bus/platform/drivers/gpio-key/quick_boot_mode fail!\n" );
	}
}

void do_bootup() {
	/*if( g_chargeanim_showed == 1 ) {
                return;
        }*/

	pthread_mutex_lock(&g_bootup_mutex);

	g_has_boot_up = 1;

	char mode[128];
        property_get("sys.qb_shutdown_mode", mode, "0");
        if( strcmp( mode, "1" ) ) {
                return;
        }

	property_set( "sys.qb_shutdown_mode", "0" );//exit qb_shutdown_mode     

	startAndroidAnim();

	//usleep(200*1000);
	
	sendQBTurnOnScreenIntent();
  
      	sleep(ANDROID_ANIM_TIME);
 
	sendQBBootIntent();

	exitGpioKeyQuickbootMode();

	exit(0);

	pthread_mutex_unlock(&g_bootup_mutex);
}

typedef void* (*pCheckLongPressFun)(void*);
void check_longpress_thread(void) {
	sleep(PWRKEY_LONGPRESS_TIME);
	if( g_powerkey_has_down == 1 ) {
		do_bootup();
	}
}

bool is_charge_full() {
    const int SIZE = 16;
    char buf[SIZE];
    bool isCharge = false;
    if(readFromFile("/sys/class/power_supply/battery/status", buf, SIZE) > 0) {
        if( !memcmp( buf, "Full", strlen("Full") ) ) {
            isCharge = true;
        }
    }
    return isCharge;
}

typedef void* (*pDoChargeAnim)(void*);
void do_charge_anim(void) {
	printf("do_charge_anim\n");
	if( g_has_boot_up == 1 ) {
		return;
	}

	if( g_chargeanim_showed == 1 ) {
		return;
	}

	g_chargeanim_showed = 1;

	if( is_charge_full() ) {
	    draw_mode = TYPE_QB_CHARGE_FULL;
	} else {
	    draw_mode = TYPE_QB_CHARGE_ANIM;
	}
        sem_post(&draw_sem);
	
	sendQBTurnOnScreenIntent();
	
	sleep(CHARGE_ANIM_TIME);

	if( g_has_boot_up == 0 ) {
		sendQBTurnOffScreenIntent();
	}
	
	g_chargeanim_showed = 0;
}

void charge_anim(void) {
	pthread_t id;
	int ret = pthread_create( &id, NULL, (pDoChargeAnim)do_charge_anim, NULL );
        if(ret < 0) {
        	fprintf(stderr, "create do_charge_anim fail!\n" );
        }
}

void update_charge_status()
{
#if 0
    const int SIZE = 16;
    char buf[SIZE];
    if (readFromFile(AC_CHARGE_PATH, buf, SIZE) > 0) {
        if (buf[0] == '0') {
            g_charge = 0;
        } else if( buf[0] == '1') {
            g_charge = 1;
        }

#if SHOW_DEBUG_LOG
        printf("update_charge_status g_charge:%d\n", g_charge);
#endif
    }
#endif
}

void dealPowerKeyEvent( int event_value ) {
#if SHOW_DEBUG_LOG
	ALOGI("quick_boot dealPowerKeyEvent event_value:%d", event_value);
#endif
	if( g_has_boot_up == 1 ) {
                return;
        }

	if( event_value == PWRKEY_UP_VALUE ) {
#if SHOW_DEBUG_LOG
		fprintf(stderr, "quickboot powerkey up! gettime:%d\n", (int)gettime() );
		ALOGI("quick_boot powerkey up! gettime:%d", (int)gettime());
#endif
		if( ( g_powerkey_has_down == 1 ) && ( g_has_boot_up == 0 ) ) {
			if( g_charge == 1 || is_charge_full() ) {
				charge_anim();
			}
		}
		g_powerkey_has_down = 0;
	} else if( event_value == PWRKEY_DOWN_VALUE ) {
#if SHOW_DEBUG_LOG
		fprintf(stderr, "quickboot powerkey down! gettime:%d\n", (int)gettime() );
		ALOGI("quick_boot powerkey down! gettime:%d", (int)gettime());
#endif		
		g_powerkey_has_down = 1;

		pthread_t id;
		int ret = pthread_create( &id, NULL, (pCheckLongPressFun)check_longpress_thread, NULL );
		if(ret < 0) {
			fprintf(stderr, "create check_longpress_thread fail!\n" );
		}
	}
}

int open_uevent_socket(void)
{
    struct sockaddr_nl addr;
    int sz = 64*1024; // XXX larger? udev uses
    int on = 1;
    int s;
    memset(&addr, 0, sizeof(addr));
    addr.nl_family = AF_NETLINK;
    addr.nl_pid = getpid();
    addr.nl_groups = 0xffffffff;
    s = socket(PF_NETLINK, SOCK_DGRAM, NETLINK_KOBJECT_UEVENT);
    if(s < 0)
        return -1;
    setsockopt(s, SOL_SOCKET, SO_RCVBUFFORCE, &sz, sizeof(sz));
    setsockopt(s, SOL_SOCKET, SO_PASSCRED, &on, sizeof(on));
    if(bind(s, (struct sockaddr *) &addr, sizeof(addr)) < 0) {
        close(s);
        return -1;
    }
    return s;
}

static void parse_event(const char *msg, struct uevent *uevent)
{
#if 1
    uevent->action = "";
    uevent->path = "";
    uevent->subsystem = "";
    uevent->firmware = "";
    uevent->major = -1;
    uevent->minor = -1;
    uevent->state = -1;
    uevent->usb_state = "";
    uevent->power_supply_state = "";
        /* currently ignoring SEQNUM */
    while(*msg) {
        if(!strncmp(msg, "ACTION=", 7)) {
            msg += 7;
            uevent->action = msg;
        } else if(!strncmp(msg, "DEVPATH=", 8)) {
            msg += 8;
            uevent->path = msg;
        } else if(!strncmp(msg, "SUBSYSTEM=", 10)) {
            msg += 10;
            uevent->subsystem = msg;
        } else if(!strncmp(msg, "FIRMWARE=", 9)) {
            msg += 9;
            uevent->firmware = msg;
        } else if(!strncmp(msg, "MAJOR=", 6)) {
            msg += 6;
            uevent->major = atoi(msg);
        } else if(!strncmp(msg, "MINOR=", 6)) {
            msg += 6;
            uevent->minor = atoi(msg);
        } else if(!strncmp(msg, "SWITCH_STATE=", 13)) {
            msg += 13;
            uevent->state = atoi(msg);
        } else if(!strncmp(msg, "USB_STATE=", 10)) {
	    msg += 10;
	    uevent->usb_state = msg;
	} else if(!strncmp(msg, "POWER_SUPPLY_STATUS=", 20)) {
	    msg += 20;
	    uevent->power_supply_state = msg;
	}

            /* advance to after the next \0 */
        while(*msg++) {
            ;
        }
    }

    if( !strcmp( uevent->action, "change") && !strcmp( uevent->path, "/devices/virtual/switch/powerkey" ) ) {
	dealPowerKeyEvent((int)uevent->state);
    }

    if( !strcmp( uevent->subsystem , "power_supply") ) {
	printf("++++++++++++++++++++++++++++power_supply:%s\n", uevent->power_supply_state);

	if( !strcmp( uevent->power_supply_state, "Charging") || !strcmp( uevent->power_supply_state, "Full") ) {
		if( g_charge == 0 ) {//plug in
			g_charge = 1;
                        charge_anim();
                } else {
			g_charge = 1;
		}
	} else if( !strcmp( uevent->power_supply_state, "Discharging") ) {
		g_charge = 0;		
	}
    }

#if 1
    printf("event { '%s', '%s', '%s', '%s', %d, %d, %d '%s' }\n",
                    uevent->action, uevent->path, uevent->subsystem,
                    uevent->firmware, uevent->major, uevent->minor, uevent->state, uevent->usb_state);
#endif
#endif
}

#define UEVENT_MSG_LEN 1024
void handle_device_fd(int fd)
{
#if SHOW_DEBUG_LOG
    ALOGI("quick_boot enter %s\n", __func__);
#endif
    for(;;) {
        char msg[UEVENT_MSG_LEN+2];
        char cred_msg[CMSG_SPACE(sizeof(struct ucred))];
        struct iovec iov = {msg, sizeof(msg)};
        struct sockaddr_nl snl;
        struct msghdr hdr = {&snl, sizeof(snl), &iov, 1, cred_msg, sizeof(cred_msg), 0};
        ssize_t n = recvmsg(fd, &hdr, 0);
        if (n <= 0) {
            break;
        }
        if ((snl.nl_groups != 1) || (snl.nl_pid != 0)) {
            /* ignoring non-kernel netlink multicast message */
            continue;
        }
        struct cmsghdr * cmsg = CMSG_FIRSTHDR(&hdr);
        if (cmsg == NULL || cmsg->cmsg_type != SCM_CREDENTIALS) {
            /* no sender credentials received, ignore message */
            continue;
        }
        struct ucred * cred = (struct ucred *)CMSG_DATA(cmsg);
        if (cred->uid != 0) {
            /* message from non-root user, ignore */
            continue;
        }
        if(n >= UEVENT_MSG_LEN) /* overflow -- discard */
            continue;
        msg[n] = '\0';
        msg[n+1] = '\0';
        struct uevent uevent;
        //printf("msg:%s\n", msg);
        parse_event(msg, &uevent);
    }
}

static void *draw_logo_manager(void *arg) {
	while(1) {
		sem_wait(&draw_sem);
		printf("draw_logo_manager enter,draw_mode:%d\n", draw_mode);
		switch(draw_mode) {
			case TYPE_QB_BOOT_ANIM:
				show_anim(TYPE_QB_BOOT_ANIM);	
				break;
			case TYPE_QB_CHARGE_ANIM:
				show_anim(TYPE_QB_CHARGE_ANIM);
				break;
			case TYPE_QB_CHARGE_FULL:
				show_anim(TYPE_QB_CHARGE_FULL);
				break;
			default:
				break;
		}
	}

	return 0;
}

void do_test() {
//test logo display with sem
#if 0
        draw_mode = TYPE_QB_CHARGE_ANIM;
        sem_post(&draw_sem);
        
        sleep(2);
        draw_mode = TYPE_QB_BOOT_ANIM;
        //draw_mode = TYPE_QB_CHARGE_ANIM;
        sem_post(&draw_sem);

        sleep(2);
        draw_mode = TYPE_QB_CHARGE_ANIM;
        sem_post(&draw_sem);

        sleep(2);
        draw_mode = TYPE_QB_BOOT_ANIM;
        //draw_mode = TYPE_QB_CHARGE_ANIM;
        sem_post(&draw_sem);
#endif

//test logo display
#if 0
        g_boot_anim = new BootAnimation();      
        g_boot_anim->showQBChargeAnim(true);
        sleep(2);
        //g_boot_anim->request_exit();
        g_boot_anim = new BootAnimation();
        g_boot_anim->showQBBootLogo(true);
#endif

//test read logo from /dev/block/logo
#if 0
	char *logo_buf = NULL;
	int nLen = read_logo("poweron", &logo_buf);

	//use logo_buf

	if(logo_buf != NULL) {
		free(logo_buf);
	}
#endif
}

int main() {
#if SHOW_DEBUG_LOG
	ALOGI("quick_boot main\n");
#endif

	enterGpioKeyQuickbootMode();

	pthread_attr_t attr;
        pthread_attr_init (&attr);
        pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
        int ret = pthread_create(&s_tid_drawlogo_manager, &attr,draw_logo_manager, NULL);
        if (ret < 0) {
                ALOGI("quick_boot create draw_logo_manager thread fail!\n");
                pthread_attr_destroy(&attr);
        }

#if 0
	do_test();
#else
	int fd = 0;
	g_has_boot_up = 0;

	fd = open_uevent_socket();
	if (fd < 0) {
        	printf("error!\n");
        	return -1;
    	}

    	handle_device_fd(fd);
#endif
	return 0;
}


