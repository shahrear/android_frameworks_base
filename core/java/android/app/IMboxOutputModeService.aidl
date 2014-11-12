package android.app;

/**
 * {@hide}
 */
interface IMboxOutputModeService {

	 void setOutputMode(String mode);
	 int[] getPosition(String mode);
	 String getBestMatchResolution();
	 void initOutputMode();
	 void setHdmiUnPlugged();
	 void setHdmiPlugged();
	 boolean isHDMIPlugged();
	 boolean ifModeIsSetting();

        int autoSwitchHdmiPassthough();
        void setDigitalVoiceValue(String value);
        void enableDobly_DRC (boolean enable);
        void setDoblyMode (String mode);
        void setDTS_DownmixMode(String mode);
        void enableDTS_DRC_scale_control (boolean enable);
        void enableDTS_Dial_Norm_control (boolean enable);
}