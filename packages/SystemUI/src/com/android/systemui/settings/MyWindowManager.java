package com.android.systemui.settings;

import android.content.Context;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;

public class MyWindowManager {

	private static final String TAG = "MyWindowManager";
	private static ClingHintView  mHintWindow;
	private static ConfigView     mConfigWindow;
	private static LayoutParams   mHintWindowParams;
	private static LayoutParams   mConfigWindowParams;
	private static WindowManager mWindowManager;

	public static void createHintWindow(Context context,String name,int version) {
		WindowManager windowManager = getWindowManager(context);
		int screenWidth = windowManager.getDefaultDisplay().getWidth();
		int screenHeight = windowManager.getDefaultDisplay().getHeight();
		if (mHintWindow == null) {
			mHintWindow = new ClingHintView(context,name,version);
			if (mHintWindowParams == null) {
				mHintWindowParams = new LayoutParams();
				mHintWindowParams.type = LayoutParams.TYPE_PHONE;
				mHintWindowParams.format = PixelFormat.RGBA_8888;
				mHintWindowParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE|LayoutParams.FLAG_FULLSCREEN;
				mHintWindowParams.gravity = Gravity.LEFT | Gravity.TOP;
				mHintWindowParams.width = screenWidth;
				mHintWindowParams.height = screenHeight+30;
				mHintWindowParams.x = 0;
				mHintWindowParams.y = 0;
			}
			windowManager.addView(mHintWindow, mHintWindowParams);
		}
	}


	public static void removeHintWindow(Context context) {
		if (mHintWindow != null) {
			WindowManager windowManager = getWindowManager(context);
			windowManager.removeView(mHintWindow);
			mHintWindow = null;
		}
	}


	public static void createConfigWindow(Context context,String name,int version) {
		WindowManager windowManager = getWindowManager(context);
		int screenWidth = windowManager.getDefaultDisplay().getWidth();
		int screenHeight = windowManager.getDefaultDisplay().getHeight();
		if (mConfigWindow == null) {
			mConfigWindow = new ConfigView(context,name,version);
			if (mConfigWindowParams == null) {
				mConfigWindowParams = new LayoutParams();
				mConfigWindowParams.x = screenWidth / 2
						- ConfigView.viewWidth / 2;
				mConfigWindowParams.y = screenHeight / 2
						- ConfigView.viewHeight / 2;
				mConfigWindowParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE;		
				mConfigWindowParams.type = LayoutParams.TYPE_PHONE;
				mConfigWindowParams.format = PixelFormat.RGBA_8888;
				mConfigWindowParams.gravity = Gravity.LEFT | Gravity.TOP;
				mConfigWindowParams.width = ConfigView.viewWidth;
				mConfigWindowParams.height = ConfigView.viewHeight;
			}
			mConfigWindow.setParams(mConfigWindowParams);
			windowManager.addView(mConfigWindow, mConfigWindowParams);
		}
	}


	public static void removeConfigWindow(Context context) {
		if (mConfigWindow != null) {
			mConfigWindow.sendCmd(0xff);
			WindowManager windowManager = getWindowManager(context);
			windowManager.removeView(mConfigWindow);
			mConfigWindow = null;
		}
	}


	private static WindowManager getWindowManager(Context context) {
		if (mWindowManager == null) {
			mWindowManager = (WindowManager) context
					.getSystemService(Context.WINDOW_SERVICE);
		}
		return mWindowManager;
	}
}
