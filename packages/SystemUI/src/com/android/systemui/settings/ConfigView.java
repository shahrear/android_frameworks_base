package com.android.systemui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.ImageButton;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import java.io.IOException;

import com.android.systemui.R;

public class ConfigView extends RelativeLayout {
	private static final String TAG = "ConfigView";

	private static final String SOCKET_ADDRESS = "custom_sensor_config";
    private WindowManager.LayoutParams wmParams;
    private WindowManager mWindowManager;
    private RelativeLayout mRelativeLayout;
    private ImageButton mBtnX,mBtnY,mBtnXY,mBtnOk;

    private String mPkgName;
    private int mVersionCode;
    private int mConfig = 0;


	public static int viewWidth;

	public static int viewHeight;

	private static int statusBarHeight = 0;

	/**
	 * 记录当前手指位置在屏幕上的横坐标值
	 */
	private float xInScreen;

	/**
	 * 记录当前手指位置在屏幕上的纵坐标值
	 */
	private float yInScreen;

	/**
	 * 记录手指按下时在屏幕上的横坐标的值
	 */
	private float xDownInScreen;

	/**
	 * 记录手指按下时在屏幕上的纵坐标的值
	 */
	private float yDownInScreen;

	/**
	 * 记录手指按下时在小悬浮窗的View上的横坐标的值
	 */
	private float xInView;

	/**
	 * 记录手指按下时在小悬浮窗的View上的纵坐标的值
	 */
	private float yInView;
	
    private boolean mReverseX = false;
    private boolean mReverseY = false;
    private boolean mReverseXY = false;

    private static final int CONFIG_REVERSE_X = 0x0001;
    private static final int CONFIG_REVERSE_Y = 0x0002;
    private static final int CONFIG_REVERSE_XY = 0x0004;   

	public ConfigView(final Context context,String name,int version) {
		super(context);
		this.mContext = mContext;
		this.mPkgName = name;
		this.mVersionCode = version;

		mWindowManager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);

		LayoutInflater.from(context).inflate(R.layout.config_view, this);
		mRelativeLayout = (RelativeLayout) findViewById(R.id.config_window_layout);

		viewWidth = mRelativeLayout.getLayoutParams().width;
		viewHeight = mRelativeLayout.getLayoutParams().height;
		Log.d(TAG,"viewWidth:"+viewWidth+"  viewHeight:"+viewHeight);
		mBtnX = (ImageButton)mRelativeLayout.findViewById(R.id.imgbtX);
		mBtnY = (ImageButton)mRelativeLayout.findViewById(R.id.imgbtY);
		mBtnXY = (ImageButton)mRelativeLayout.findViewById(R.id.imgbtXY);
		mBtnOk = (ImageButton)mRelativeLayout.findViewById(R.id.imgbtOK);
			
		mRelativeLayout.setOnTouchListener(new OnTouchListener(){

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					// 手指按下时记录必要数据,纵坐标的值都需要减去状态栏高度
					xInView = event.getX();
					yInView = event.getY();
					xDownInScreen = event.getRawX();
					yDownInScreen = event.getRawY() - getStatusBarHeight();
					xInScreen = event.getRawX();
					yInScreen = event.getRawY() - getStatusBarHeight();
					break;
				case MotionEvent.ACTION_MOVE:
					xInScreen = event.getRawX();
					yInScreen = event.getRawY() - getStatusBarHeight();
					// 手指移动的时候更新小悬浮窗的状态和位置
					updateViewPosition();
					break;
				case MotionEvent.ACTION_UP:
					updateViewPosition();
					break;
				default:
					break;
				}
				return true;
			}
			
		});
		
		mBtnX.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mReverseX = !mReverseX;
				if(mReverseX){
					mBtnX.setBackgroundResource(R.drawable.vertical_hl);
					mConfig |= CONFIG_REVERSE_X;
				}else{
					mBtnX.setBackgroundResource(R.drawable.vertical);
					mConfig &= ~CONFIG_REVERSE_X;
				}
				sendCmd(mConfig);
			}
			
		});

		mBtnY.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mReverseY = !mReverseY;
				
				if(mReverseY){
					mBtnY.setBackgroundResource(R.drawable.horizontal_hl);
					mConfig |= CONFIG_REVERSE_Y;
				}else{
					mBtnY.setBackgroundResource(R.drawable.horizontal);
					mConfig &= ~CONFIG_REVERSE_Y;
				}
				sendCmd(mConfig);
			}
			
		});

		mBtnXY.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mReverseXY = !mReverseXY;
				
				if(mReverseXY){
					mBtnXY.setBackgroundResource(R.drawable.rotation_hl);
					mConfig |= CONFIG_REVERSE_XY;
				}else{
					mBtnXY.setBackgroundResource(R.drawable.rotation);
					mConfig &= ~CONFIG_REVERSE_XY;
				}
				sendCmd(mConfig);
			}
			
		});


	    mBtnOk.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					mBtnOk.setBackgroundResource(R.drawable.ok_hl);
					insertRecord(mPkgName,mVersionCode,mConfig);
					Intent intent = new Intent("android.intent.action.gSensorBroadcast");
                    intent.putExtra("show",false);
                    Log.d(TAG,"===save  sendBroadcast");
                    context.sendBroadcast(intent);
				}
	        });
	    initStatus();
	}


	public void setParams(WindowManager.LayoutParams params) {
		wmParams = params;
	}

		/**
	 * 更新小悬浮窗在屏幕中的位置。
	 */
	private void updateViewPosition() {
		wmParams.x = (int) (xInScreen - xInView);
		wmParams.y = (int) (yInScreen - yInView);
		mWindowManager.updateViewLayout(this, wmParams);
	}
	
	/**
	 * 用于获取状态栏的高度。
	 * 
	 * @return 返回状态栏高度的像素值。
	 */
	private int getStatusBarHeight() {
		if (statusBarHeight == 0) {
			statusBarHeight = mContext.getResources()
			                  .getDimensionPixelSize(com.android.internal.R.dimen.status_bar_height);
		}
		return statusBarHeight;
	}
	
	private void initStatus(){
	
		ContentResolver  resolver =  mContext.getContentResolver();
		Cursor c = resolver.query(MyApps.App.CONTENT_URI, null, "AKG_NAME="+ "'"+ mPkgName+ "'", null, null);
		if(c!=null&&c.moveToFirst()){
			mConfig = c.getInt(3);
			Log.d(TAG,"===sensorCfg: "+mConfig);
		}
		c.close();		
		
		if( (mConfig & CONFIG_REVERSE_X) > 0 ){
			mReverseX = true;
			mBtnX.setBackgroundResource(R.drawable.vertical_hl);
		}

		if( (mConfig & CONFIG_REVERSE_Y) > 0 ){
			mReverseY = true;
			mBtnY.setBackgroundResource(R.drawable.horizontal_hl);
		}
		
		if( (mConfig & CONFIG_REVERSE_XY) > 0 ){
			mReverseXY = true;
			mBtnXY.setBackgroundResource(R.drawable.rotation_hl);
		}
	}

	private void insertRecord(String pkgName,int versionCode,int sensorCfg){
		ContentValues  values = new ContentValues();
		ContentResolver  resolver =  mContext.getContentResolver();
		values.put(MyApps.App.AKG_NAME, pkgName);
		values.put(MyApps.App.VERSION_CODE, versionCode);
		values.put(MyApps.App.SENSOR_CFG, sensorCfg);
		
		Cursor c = resolver.query(MyApps.App.CONTENT_URI, null, "AKG_NAME="+ "'"+ pkgName+ "'", null, null);
		if(c.moveToFirst()){
			Log.d(TAG,"the name is already exist!!!!");
			resolver.update(MyApps.App.CONTENT_URI, values, "AKG_NAME="+ "'"+ pkgName+ "'", null);
		}else{
			resolver.insert(MyApps.App.CONTENT_URI, values);
		}
		c.close();
	}
	
	public void sendCmd(int config){
		LocalSocket sender = new LocalSocket();
		try {
			sender.connect(new LocalSocketAddress(SOCKET_ADDRESS));
			sender.getOutputStream().write(config);
			sender.getOutputStream().close();
			sender.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}	
}
