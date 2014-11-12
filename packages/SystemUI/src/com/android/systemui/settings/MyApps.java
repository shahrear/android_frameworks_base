package com.android.systemui.settings;

import android.net.Uri;
import android.provider.BaseColumns;

public class MyApps {
	public static final String AUTHORITY = "com.android.systemui.settings.sensorCfg";
	
	public static final class App implements BaseColumns{
		public static final Uri CONTENT_URI = Uri.parse("content://com.android.systemui.settings.sensorCfg");
		
		public static final String AKG_NAME = "AKG_NAME";
		public static final String VERSION_CODE = "VERSION_CODE";
		public static final String SENSOR_CFG = "SENSOR_CFG";
		
	}

}
