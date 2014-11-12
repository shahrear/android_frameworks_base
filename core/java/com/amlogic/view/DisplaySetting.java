/*
 *data: 2014.01.08
 *author: jeff.yang
 *relese: init
 *description: give a summarize to the added functions before.
*/


package com.amlogic.view;

import android.util.Log;
import android.view.Surface;
import android.view.Display;
import android.os.RemoteException;
import android.os.IBinder;
import android.view.IWindowManager;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;


public class DisplaySetting
{
	public static final int REQUEST_DISPLAY_FORMAT_1080P=0;
	public static final int REQUEST_DISPLAY_FORMAT_1080I=1;
	public static final int REQUEST_DISPLAY_FORMAT_720P=2;
	public static final int REQUEST_DISPLAY_FORMAT_720I=3;
	public static final int REQUEST_DISPLAY_FORMAT_576P=4;
	public static final int REQUEST_DISPLAY_FORMAT_576I=5;
	public static final int REQUEST_DISPLAY_FORMAT_480P=6;
	public static final int REQUEST_DISPLAY_FORMAT_480I=7;

	public static final int STEREOSCOPIC_3D_FORMAT_OFF=0;
	public static final int STEREOSCOPIC_3D_FORMAT_SIDE_BY_SIDE=1;
	public static final int STEREOSCOPIC_3D_FORMAT_TOP_BOTTOM=2;
	public static final int STEREOSCOPIC_3D_FORMAT_INTERLEAVED=4;
	public static final int REQUEST_3D_FORMAT_SIDE_BY_SIDE=8;
	public static final int REQUEST_3D_FORMAT_TOP_BOTTOM=16;

	public DisplaySetting()
	{
	
	}
	
	public static boolean setDisplaySize(int format)
	{
        IWindowManager mIWindowManager = WindowManagerGlobal.getWindowManagerService();
        try {
		    mIWindowManager.setVDisplaySize(0,format);
		}catch (RemoteException e) {
		}
		
		return true;
	}
	
	public static boolean setSurface2Stereoscopic(android.view.Surface surface,int format)
	{
		return true;
	}
	
	//have not tested yet
	public static boolean setDisplay2Stereoscopic(int format)
	{
	   
	    IWindowManager mIWindowManager = WindowManagerGlobal.getWindowManagerService();
        try {
            Log.v("yjf setDisplay2Stereoscopic"," format"+format);
		    mIWindowManager.setDisplay2Stereoscopic(0,format);
		}catch (RemoteException e) {
		}
		
		return true;
	}	
	
	public static boolean setVideoHole(android.view.Surface surface)
	{
		return true;
	}		
	
}
