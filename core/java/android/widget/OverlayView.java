package android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RemoteViews.RemoteView;
import android.app.OverlayViewManager;
import android.util.Log;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.KeyEvent;
import java.lang.Thread;
import android.view.View;
import android.content.ComponentName;
import android.content.Intent;

import android.os.SystemProperties;

import android.os.Handler;
import android.os.Message;
import java.util.Timer;
import java.util.TimerTask;

import android.view.Surface;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

// @RemoteView
// public class OverlayView extends ImageView
public class OverlayView extends SurfaceView
{
	private Context mContext = null;
	private static String TAG = "OverlayView";
	
	private OverlayViewManager mOverlayViewManager = null;
	
//	private TimerTask mTimerTask = null;
//	private Timer mTimer = null;
//	private Handler mHandler = null;
	
//	private int pip_toggle_flag = 0;
//	private int audio_enable = 0;
	
	public OverlayView(Context context) 
	{
		super(context);
		
		mContext = context;
		
		mOverlayViewManager = (OverlayViewManager)context.getSystemService(Context.OVERLAYVIEW_SERVICE);
	}
	
	public OverlayView(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		
		mContext = context;
		
		mOverlayViewManager = (OverlayViewManager)context.getSystemService(Context.OVERLAYVIEW_SERVICE);
	}
	
	public OverlayView(Context context, AttributeSet attrs, int defStyle) 
	{
		super(context, attrs, defStyle);
		
		mContext = context;
		
		mOverlayViewManager = (OverlayViewManager)context.getSystemService(Context.OVERLAYVIEW_SERVICE);
	}
	
//	private void initContext()
//	{
		// startAudioHandleTimer();
		// startTimer();
//	}
/*	
    private void startAudioHandleTimer()
	{
    		final Handler mAudioHandler = new Handler()
			{  
			       public void handleMessage(Message msg)
				   {  
				   		// hdmi_api.handle_audio();
			    	   
				     	super.handleMessage(msg);
				     	
				     	handleAudio();
			       }
		   };  
			
		  Timer mAudioTimer = new Timer();
		  mAudioTimer.schedule(new TimerTask()
		  {  
	    	    public void run() 
				{  
	    		    Message message = new Message();      
		            message.what = 0;			//anything is ok.
		            mAudioHandler.sendMessage(message); 
	            }
			}, 0, 20);
    }
*/
/*	
    private void startTimer()
	{
		mHandler = new Handler()
		{  
			 public void handleMessage(Message msg)
			 {  
				 String info_str = "";
				 
//				 int width = hdmi_api.get_h_active();
//
//				 int height = hdmi_api.get_v_active();
//
//				 boolean is_dvi = hdmi_api.is_dvi();
//
//				 boolean is_interlace = hdmi_api.is_interlace();
//
//				 info_str = info_str.format("%dx%d%s%s", width, is_interlace?height*2:height, is_interlace?"I":"P", is_dvi?"(DVI)":"");
//
//				 getWindow().setTitle(info_str);
				 
				 super.handleMessage(msg);
				 
				 int width = getHActive();
				 int height = getVActive();
				 boolean isDvi = isDvi();
				 boolean isInterlace = isInterlace();
				 
				 info_str = info_str.format("%dx%d%s%s", width, 
						 isInterlace ? height*2 : height, 
						 isInterlace ? "I" : "P", isDvi ? "(DVI)" : "");
			 }
		};
		
	    mTimerTask = new TimerTask()
		{  
	    	public void run() 
			{  
	    		 Message message = new Message();      
		         message.what = 0;			//anything is ok.
		         mHandler.sendMessage(message); 
	         }  
	    };  
		
		mTimer = new Timer();
		mTimer.schedule(mTimerTask, 0, 1000);
	}
*/
	
	public void init(int source)
	{
		mOverlayViewManager.init(source);
		
		// initContext();
	}
	
	public void deinit()
	{
		mOverlayViewManager.deinit();
	}
	
    public int displayHdmi()
    {
    	return mOverlayViewManager.displayHdmi();
    }
    
    public int displayAndroid()
    {
    	return mOverlayViewManager.displayAndroid();
    }
    
    public int displayPip(int x, int y, int width, int height)
    {
 //   	SystemProperties.set("mbx.hdmiin.audio", "true");
    	
    	return mOverlayViewManager.displayPip(x, y, width, height);
    }
    
    public int getHActive()
    {
    	return mOverlayViewManager.getHActive();
    }
    
    public int getVActive()
    {
    	return mOverlayViewManager.getVActive();
    }

    public String getHdmiInSize()
    {
        return mOverlayViewManager.getHdmiInSize();
    }
    
    public boolean isDvi()
    {
    	return mOverlayViewManager.isDvi();
    }

    public boolean isPowerOn()
    {
    	return mOverlayViewManager.isPowerOn();
    }

    public boolean isEnable()
    {
    	return mOverlayViewManager.isEnable();
    }
    
    public boolean isInterlace()
    {
    	return mOverlayViewManager.isInterlace();
    }
    
    public boolean hdmiPlugged()
    {
    	return mOverlayViewManager.hdmiPlugged();
    }
    
    public boolean hdmiSignal()
    {
    	return mOverlayViewManager.hdmiSignal();
    }
    
    public int enableAudio(int flag)
    {
    	return mOverlayViewManager.enableAudio(flag);
    }
    
    public int handleAudio()
    {
    	return mOverlayViewManager.handleAudio();
    }
    
    public void setEnable(boolean enable)
    {
    	mOverlayViewManager.setEnable(enable);
    }
    
    public int setSourceType()
    {
    	return mOverlayViewManager.setSourceType();
    }

    public boolean isSurfaceAvailable(Surface sur) {
        return mOverlayViewManager.isSurfaceAvailable(sur);
    }

    public boolean setPreviewWindow(Surface sur) {
        return mOverlayViewManager.setPreviewWindow(sur);
    }

	public int setCrop(int x, int y, int width, int height) {
		return mOverlayViewManager.setCrop(x, y, width, height);
	}

    public void startMov() {
        mOverlayViewManager.startMov();
    }
    
    public void stopMov() {
        mOverlayViewManager.stopMov();
    }

    public void pauseMov() {
        mOverlayViewManager.pauseMov();
    }
    
    public void resumeMov() {
        mOverlayViewManager.resumeMov();
    }
}
