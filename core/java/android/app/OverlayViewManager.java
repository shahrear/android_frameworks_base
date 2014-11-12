package android.app;

import android.os.RemoteException;
import android.view.Surface;

//single instance mode per process

public class OverlayViewManager
{
	private IOverlayView mService = null;
	
	OverlayViewManager(IOverlayView service)
	{
		mService = service;
	}
	
	public void init(int source)
	{
		try
		{
			mService.init(source);
		}
		catch(RemoteException e)
		{
			e.printStackTrace();
		}
	}
	
	public void deinit()
	{
		try
		{
			mService.deinit();
		}
		catch(RemoteException e)
		{
			e.printStackTrace();
		}
	}
	
    public int displayHdmi()
    {
		int result = 0;

    	try
    	{
			result = mService.displayHdmi();
    	}
    	catch(RemoteException e)
    	{
    		e.printStackTrace();
    	}

		return result;
    }
    
    public int displayAndroid()
    {
		int result = 0;

    	try
    	{
    		result = mService.displayAndroid();
    	}
    	catch(RemoteException e)
    	{
    		e.printStackTrace();
    	}

		return result;
    }
    
    public int displayPip(int x, int y, int width, int height)
    {
		int result = 0;

    	try
    	{
    		result = mService.displayPip(x, y, width, height);
    	}
    	catch(RemoteException e)
    	{
    		e.printStackTrace();
    	}
	
		return result;
    }
    
    public int getHActive()
    {
		int result = 0;

    	try
    	{
    		result = mService.getHActive();
    	}
    	catch(RemoteException e)
    	{
    		e.printStackTrace();
    	}

		return result;
    }
    
    public int getVActive()
    {
		int result = 0;

    	try
    	{
    		result = mService.getVActive();
    	}
    	catch(RemoteException e)
    	{
    		e.printStackTrace();
    	}

		return result;
    }

	public String getHdmiInSize()
	{
		String result = "";

		try
		{
			result = mService.getHdmiInSize();
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
		}

		return result;
	}
    
    public boolean isDvi()
    {
		boolean result = false;

    	try
    	{
    		result = mService.isDvi();
    	}
    	catch(RemoteException e)
    	{
    		e.printStackTrace();
    	}

		return result;
    }

    public boolean isPowerOn()
    {
		boolean result = false;

    	try
    	{
    		result = mService.isPowerOn();
    	}
    	catch(RemoteException e)
    	{
    		e.printStackTrace();
    	}
		return result;
    }

    public boolean isEnable()
    {
        boolean result = false;

        try
        {
            result = mService.isEnable();
        }
        catch(RemoteException e)
        {
            e.printStackTrace();
        }
        return result;
    }
    
    public boolean isInterlace()
    {
		boolean result = false;

    	try
    	{
    		result = mService.isInterlace();
    	}
    	catch(RemoteException e)
    	{
    		e.printStackTrace();
    	}

		return result;
    }
    
    public boolean hdmiPlugged()
    {
		boolean result = false;

    	try
    	{
    		result = mService.hdmiPlugged();
    	}
    	catch(RemoteException e)
    	{
    		e.printStackTrace();
    	}

		return result;
    }
    
    public boolean hdmiSignal()
    {
		boolean result = false;

    	try
    	{
    		result = mService.hdmiSignal();
    	}
    	catch(RemoteException e)
    	{
    		e.printStackTrace();
    	}

		return result;
    }
    
    public int enableAudio(int flag)
    {
		int result = 0;

    	try
    	{
    		result = mService.enableAudio(flag);
    	}
    	catch(RemoteException e)
    	{
    		e.printStackTrace();
    	}

		return result;
    }
    
    public int handleAudio()
    {
		int result = 0;

    	try
    	{
    		result = mService.handleAudio();
    	}
    	catch(RemoteException e)
    	{
    		e.printStackTrace();
    	}

		return result;
    }
    
    public void setEnable(boolean enable)
    {
    	try
    	{
    		mService.setEnable(enable);
    	}
    	catch(RemoteException e)
    	{
    		e.printStackTrace();
    	}
    }
    
    public int setSourceType()
    {
		int result = 0;

    	try
    	{
    		result = mService.setSourceType();
    	}
    	catch(RemoteException e)
    	{
    		e.printStackTrace();
    	}

		return result;
    }

    public boolean isSurfaceAvailable(Surface surface) {
        boolean result = false;

        try
        {
            result = mService.isSurfaceAvailable(surface);
        }
        catch(RemoteException e)
        {
            e.printStackTrace();
        }
        return result;
    }

    public boolean setPreviewWindow(Surface surface) {
        boolean result = false;

        try
        {
            result = mService.setPreviewWindow(surface);
        }
        catch(RemoteException e)
        {
            e.printStackTrace();
        }
        return result;
    }
    
    public int setCrop(int x, int y, int width, int height)
    {
		int result = 0;

    	try
    	{
    		result = mService.setCrop(x, y, width, height);
    	}
    	catch(RemoteException e)
    	{
    		e.printStackTrace();
    	}
	
		return result;
    }

    public void startMov() {
        try
        {
            mService.startMov();
        }
        catch(RemoteException e)
        {
            e.printStackTrace();
        }
    }
    
    public void stopMov() {
        try
        {
            mService.stopMov();
        }
        catch(RemoteException e)
        {
            e.printStackTrace();
        }
    }

    public void pauseMov() {
        try
        {
            mService.pauseMov();
        }
        catch(RemoteException e)
        {
            e.printStackTrace();
        }
    }
    
    public void resumeMov() {
        try
        {
            mService.resumeMov();
        }
        catch(RemoteException e)
        {
            e.printStackTrace();
        }
    }
}
