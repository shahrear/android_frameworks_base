package com.android.systemui.settings;

import java.util.List;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.android.systemui.R;

public class RocketView {
	public static final String TAG = "RocketView";
	private Context mContext;
	private WindowManager mWindowManger;
	private ActivityManager mActivityManager;
	private WindowManager.LayoutParams  mRocketParams;
	private LinearLayout mRocketLayout;
	private AnimationDrawable mAnimationDrawable;
	private long mAvailMem;
	private ImageView mRocket;
	
	public RocketView(Context mContext) {
		super();
		this.mContext = mContext;
	}
	
	public void CreateView(){
		mWindowManger = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
		mRocketParams = new WindowManager.LayoutParams();
		LayoutInflater inflater = LayoutInflater.from(mContext);
		mRocketLayout = (LinearLayout)inflater.inflate(R.layout.rocket, null);
		mRocket = (ImageView) mRocketLayout.findViewById(R.id.rocket_img);

		
		int screenWidth = mWindowManger.getDefaultDisplay().getWidth();
		int screenHeight = mWindowManger.getDefaultDisplay().getHeight();
		
		mRocketParams.x = screenWidth / 2 - mRocket.getLayoutParams().width/2;
		mRocketParams.y = screenHeight/2 - mRocket.getLayoutParams().height/2;
		mRocketParams.type = LayoutParams.TYPE_PHONE;
		mRocketParams.format = PixelFormat.RGBA_8888;
		mRocketParams.gravity = Gravity.LEFT | Gravity.TOP;
		mRocketParams.width = mRocket.getLayoutParams().width;
		mRocketParams.height = mRocket.getLayoutParams().height;	
		
		mWindowManger.addView(mRocketLayout, mRocketParams);
		new LaunchTask().execute();
		
	}
	
	
	
	class LaunchTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {

			mAvailMem = getAvailableMemory();
			releaseMemory(mContext);
			System.gc();
			Log.d(TAG,"Release Memory ok!");
			//mRocket.setImageResource(R.drawable.rocket_anim);
            mAnimationDrawable = (AnimationDrawable) mRocket.getDrawable();
            mAnimationDrawable.start();
			int i = mAnimationDrawable.getNumberOfFrames();
			while ( i-- > 0) {
				try {
					Thread.sleep(40);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Void... values) {

		}

		@Override
		protected void onPostExecute(Void result) {
			mAnimationDrawable.stop();
			mWindowManger.removeView(mRocketLayout);
			long releaseMem = getAvailableMemory() - mAvailMem;
			int percent = (int) (releaseMem / (float) mAvailMem * 100);
			releaseMem = releaseMem>>20;
			if(percent <= 0){
				Toast.makeText(mContext, " Performance has been in the best state! " , Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText(mContext, " Release Memory " + releaseMem +" MB" + "\n Performance improve " + percent +"% " , Toast.LENGTH_SHORT).show();
			}

		}

	}
	
	private ActivityManager getActivityManager(Context context) {
		if (mActivityManager == null) {
			mActivityManager = (ActivityManager) context
					.getSystemService(Context.ACTIVITY_SERVICE);
		}
		return mActivityManager;
	}	
	
	
	private long getAvailableMemory() {
		ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
		getActivityManager(mContext).getMemoryInfo(mi);
		return mi.availMem;
	}
	
	public static void releaseMemory(Context context){
		ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningAppProcessInfo> list = activityManager.getRunningAppProcesses();
		if(list!=null){
			for(int i=0; i< list.size();i++){
				ActivityManager.RunningAppProcessInfo apinfo = list.get(i);
				String[] pkgList = apinfo.pkgList;
				if(apinfo.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE ){
					for(int j=0;j<pkgList.length;j++){
						Log.i(TAG,"forceStopPackage: "+pkgList[j]);
						activityManager.forceStopPackage(pkgList[j]);
					}
				}
			}
		}
		
	}

}
