package android.app;
import android.content.Context;
import android.content.Intent;
import android.util.Slog;
import android.os.RemoteException;
import android.os.ServiceManager;
import java.util.ArrayList;

/**
 * {@hide}
 */
public class MboxOutputModeManager{
    private final IMboxOutputModeService mService ;
    private String TAG = "MboxOutputModeManager";
     /**
        * @hide
        */
    MboxOutputModeManager(IMboxOutputModeService service) {
        mService = service;
    }
     
     /**
        * @hide
        */
    public void setOutputMode(final String mode){
        try {
            mService.setOutputMode(mode);
        } catch (RemoteException ex) {
            Slog.e(TAG,"changeOutputMode error!");
        }
     }
     
     /**
        * @hide
        */
     public int[] getPosition(String mode){
        try {
            int[] position = mService.getPosition(mode);
            return position;
        } catch (RemoteException ex) {
            Slog.e(TAG,"getPosition error!");
            return null;
        }
     }
     
     /**
        * @hide
        */
     public String getBestMatchResolution() {
        try {
            String string = mService.getBestMatchResolution();
            return string;
        } catch (RemoteException ex) {
            Slog.e(TAG,"getBestMatchResolution error!");
            return null;
        }
     }

     /**
        * @hide
        */
     public void initOutputMode(){
        try {
            mService.initOutputMode();
        } catch (RemoteException ex) {
            Slog.e(TAG,"nitOutputMode error!");
        }
     }

     /**
        * @hide
        */
     public void setHdmiUnPlugged(){
        try {
            mService.setHdmiUnPlugged();
        } catch (RemoteException ex) {
            Slog.e(TAG,"hdmiUnPlugged error!");
        }
     }

     /**
        * @hide
        */
     public void setHdmiPlugged(){
        try {
            mService.setHdmiPlugged();
        } catch (RemoteException ex) {
            Slog.e(TAG,"hdmiPlugged error!");
        }
     }
     
     /**
        * @hide
        */
     public boolean isHDMIPlugged(){
        try {
            boolean plugged = mService.isHDMIPlugged();
            return plugged;
        } catch (RemoteException ex) {
            Slog.e(TAG,"isHDMIPlugged error!");
            return true;
        }
     }

          
     /**
        * @hide
        */
     public boolean ifModeIsSetting(){
        try {
            boolean ifModeSetting = mService.ifModeIsSetting();
            return ifModeSetting;
        } catch (RemoteException ex) {
            Slog.e(TAG,"ifModeIsSetting error!");
            return false;
        }
     }
	 
     /**
        * @hide
        */
     public int autoSwitchHdmiPassthough(){
        try {
            return mService.autoSwitchHdmiPassthough();
        } catch (RemoteException ex) {
            Slog.e(TAG,"autoSwitchHdmiPassthough error!");
            return -1;
        }
     }
	 
     /**
        * @hide
        */
     public void setDigitalVoiceValue(String value){
        try {
            mService.setDigitalVoiceValue(value);
        } catch (RemoteException ex) {
            Slog.e(TAG,"setDigitalVoiceValue error!");
        }
     }
	 
     /**
        * @hide
        */
     public void enableDobly_DRC (boolean enable){
        try {
            mService.enableDobly_DRC(enable);
        } catch (RemoteException ex) {
            Slog.e(TAG,"enableDobly_DRC error!");
        }
     }
	 
     /**
        * @hide
        */
     public void setDoblyMode (String mode){
        try {
            mService.setDoblyMode(mode);
        } catch (RemoteException ex) {
            Slog.e(TAG,"setDoblyMode error!");
        }
     }
	 
     /**
        * @hide
        */
     public void setDTS_DownmixMode(String mode){
        try {
            mService.setDTS_DownmixMode(mode);
        } catch (RemoteException ex) {
            Slog.e(TAG,"setDTS_DownmixMode error!");
        }
     }
	 
     /**
        * @hide
        */
     public void enableDTS_DRC_scale_control (boolean enable){
        try {
            mService.enableDTS_DRC_scale_control(enable);
        } catch (RemoteException ex) {
            Slog.e(TAG,"enableDTS_DRC_scale_control error!");
        }
     }
	 
     /**
        * @hide
        */
     public void enableDTS_Dial_Norm_control (boolean enable){
        try {
            mService.enableDTS_Dial_Norm_control(enable);
        } catch (RemoteException ex) {
            Slog.e(TAG,"enableDTS_Dial_Norm_control error!");
        }
     }
}
