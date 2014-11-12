package com.android.server;

import android.content.Context;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.app.IOverlayView;
import android.view.Surface;

public class OverlayViewService extends IOverlayView.Stub
{
	private static final String mTag = "OverlayViewService";
	private Context mContext = null;
	
	public OverlayViewService(Context context)
	{
		mContext = context;
	}

    private boolean getOverlayViewEnable() {
        boolean ret = false;
        ret = SystemProperties.getBoolean("ro.app.overlayviewE",false);
        return ret;
    }
	
    @Override
    public void init(int source) throws RemoteException {
        // TODO Auto-generated method stub

        if(true == getOverlayViewEnable())
            _init(source);
    }
	
    @Override
    public void deinit() throws RemoteException {

        if(true == getOverlayViewEnable())
            _deinit();
    }

    @Override
    public int displayHdmi() throws RemoteException {
        // TODO Auto-generated method stub
        int ret = -1;
        if(true == getOverlayViewEnable())
            ret = _displayHdmi();
        return ret;
    }

    @Override
    public int displayAndroid() throws RemoteException {
        // TODO Auto-generated method stub
        int ret = -1;
        if(true == getOverlayViewEnable())
            ret = _displayAndroid();
        return ret;
    }

    @Override
    public int displayPip(int x, int y, int width, int height) throws RemoteException {
        // TODO Auto-generated method stub
        int ret = -1;
        if(true == getOverlayViewEnable())
            ret = _displayPip(x, y, width, height);
        return ret;
    }

    @Override
    public int getHActive() throws RemoteException {
        // TODO Auto-generated method stub
        int ret = -1;
        if(true == getOverlayViewEnable())
            ret = _getHActive();
        return ret;
    }

    @Override
    public int getVActive() throws RemoteException {
        // TODO Auto-generated method stub
        int ret = -1;
        if(true == getOverlayViewEnable())
            ret = _getVActive();
        return ret;
    }

	@Override
	public String getHdmiInSize() throws RemoteException {
		String ret = "";
		if (true == getOverlayViewEnable())
			ret = _getHdmiInSize();
		return ret;
	}

    @Override
    public boolean isDvi() throws RemoteException {
        // TODO Auto-generated method stub
        boolean ret = false;
        if(true == getOverlayViewEnable())
            ret = _isDvi();
        return ret;
    }

    @Override
    public boolean isPowerOn() throws RemoteException {
        // TODO Auto-generated method stub
        boolean ret = false;
        if(true == getOverlayViewEnable())
            ret = _isPowerOn();
        return ret;
    }

    @Override
    public boolean isEnable() throws RemoteException {
        // TODO Auto-generated method stub
        boolean ret = false;
        if(true == getOverlayViewEnable())
            ret = _isEnable();
        return ret;
    }

    @Override
    public boolean isInterlace() throws RemoteException {
        // TODO Auto-generated method stub
        boolean ret = false;
        if(true == getOverlayViewEnable())
            ret = _isInterlace();
        return ret;
    }

    @Override
    public boolean hdmiPlugged() throws RemoteException {
        // TODO Auto-generated method stub
        boolean ret = false;
        if(true == getOverlayViewEnable())
            ret = _hdmiPlugged();
        return ret;
    }

    @Override
    public boolean hdmiSignal() throws RemoteException {
        // TODO Auto-generated method stub
        boolean ret = false;
        if(true == getOverlayViewEnable())
            ret = _hdmiSignal();
        return ret;
    }

    @Override
    public int enableAudio(int flag) throws RemoteException {
        // TODO Auto-generated method stub
        int ret = -1;
        if(true == getOverlayViewEnable())
            ret = _enableAudio(flag);
        return ret;
    }

    @Override
    public int handleAudio() throws RemoteException {
        // TODO Auto-generated method stub
        int ret = -1;
        if(true == getOverlayViewEnable())
            ret = _handleAudio();
        return ret;
    }

    @Override
    public void setEnable(boolean enable) throws RemoteException {
        // TODO Auto-generated method stub
        if(true == getOverlayViewEnable())
            _setEnable(enable);
    }

    @Override
    public int setSourceType() throws RemoteException {
        // TODO Auto-generated method stub
        int ret = -1;
        if(true == getOverlayViewEnable())
            ret = _setSourceType();
        return ret;
    }

    @Override
    public boolean isSurfaceAvailable(Surface surface) {
        boolean ret = false;
        if(true == getOverlayViewEnable())
            ret = _isSurfaceAvailable(surface);
        return ret;
    }

    @Override
    public boolean setPreviewWindow(Surface surface) {
        boolean ret = false;
        if(true == getOverlayViewEnable())
            ret = _setPreviewWindow(surface);
        return ret;
    }

    @Override
    public int setCrop(int x, int y, int width, int height) throws RemoteException {
        // TODO Auto-generated method stub
        int ret = -1;
        if(true == getOverlayViewEnable())
            ret = _setCrop(x, y, width, height);
        return ret;
    }

    public void startMov() {
        if(true == getOverlayViewEnable())
            _startMov();
    }
    
    public void stopMov() {
        if(true == getOverlayViewEnable())
            _stopMov();
    }

    public void pauseMov() {
        if(true == getOverlayViewEnable())
            _pauseMov();
    }
    
    public void resumeMov() {
        if(true == getOverlayViewEnable())
            _resumeMov();
    }

    private native void _init(int source);
    private native void _deinit();
    private native int _displayHdmi();
    private native int _displayAndroid();
    private native int _displayPip(int x, int y, int width, int height);
    private native int _getHActive();
    private native int _getVActive();
    private native String _getHdmiInSize();
    private native boolean _isDvi();
    private native boolean _isPowerOn();
    private native boolean _isEnable();
    private native boolean _isInterlace();
    private native boolean _hdmiPlugged();
    private native boolean _hdmiSignal();
    private native int _enableAudio(int flag);
    private native int _handleAudio();
    private native void _setEnable(boolean enable);
    private native int _setSourceType();
    private native boolean _isSurfaceAvailable(Surface surface);
    private native boolean _setPreviewWindow(Surface surface);
    private native int _setCrop(int x, int y, int width, int height);
    private native void _startMov();
    private native void _stopMov();
    private native void _pauseMov();
    private native void _resumeMov();
}
