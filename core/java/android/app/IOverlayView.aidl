package android.app;

import android.view.Surface;

interface IOverlayView
{
	void init(int source);
	
	int displayHdmi();
    
	int displayAndroid();
    
	int displayPip(int x, int y, int width, int height);
    
	int getHActive();
    
	int getVActive();

	String getHdmiInSize();
    
	boolean isDvi();
	
	boolean isPowerOn();
	
	boolean isEnable();
    
	boolean isInterlace();
    
	boolean hdmiPlugged();
    
	boolean hdmiSignal();
    
	int enableAudio(int flag);
    
	int handleAudio();
    
	void setEnable(boolean enable);
    
	int setSourceType();
	
	void deinit();
	
	boolean isSurfaceAvailable(in Surface surface);
	
	boolean setPreviewWindow(in Surface surface);
    
	int setCrop(int x, int y, int width, int height);
	
	void startMov();
	
	void stopMov();
	
	void pauseMov();
	
	void resumeMov();
}
