package com.android.systemui.settings;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.TextView;
import android.widget.RelativeLayout;
import android.util.Log;
import android.text.Html;

import com.android.systemui.R;

public class ClingHintView extends RelativeLayout {

	private static final String TAG = "ClingHintView";
	private Context mContext;
	private String  mName;
	private int  mVersion;
    private WindowManager.LayoutParams wmParams;
    private WindowManager mWindowManager;
    private RelativeLayout mRelativeLayout;

	public static int viewWidth;
	public static int viewHeight;

	public ClingHintView(Context context,String name,int version) {
		super(context);
		mContext = context;
		mName = name;
		mVersion = version;
		mWindowManager = (WindowManager) context
				.getSystemService(Context.WINDOW_SERVICE);
	
		LayoutInflater.from(context).inflate(R.layout.hint_view, this);
		mRelativeLayout = (RelativeLayout) findViewById(R.id.hint_window_layout);
		TextView tv_hint = (TextView)findViewById(R.id.tv_hint1);
		String hint_str = context.getResources().getString(R.string.cling_hint);
		String hint_str1 = context.getResources().getString(R.string.cling_hint1);
		String hint_str2 = context.getResources().getString(R.string.cling_hint2);
		String hint = hint_str + "<font color=\"#49C0EC\"><B>"+hint_str1+"</B></font>"+hint_str2;
		Log.d(TAG,"hint string:"+hint);
		//hint_str = "以下三种情况可<font color=\"#49C0EC\"><B>组合选择</B></font>,按OK键完成配置";
		tv_hint.setText(Html.fromHtml(hint));  
		viewWidth = mRelativeLayout.getLayoutParams().width;
		viewHeight = mRelativeLayout.getLayoutParams().height;
		Button btn = (Button)findViewById(R.id.dismiss);
		btn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				MyWindowManager.removeHintWindow(mContext);
				MyWindowManager.createConfigWindow(mContext,mName,mVersion);
			}
			
		});
		Log.d(TAG,"viewWidth:"+viewWidth+"  viewHeight:"+viewHeight);
	}

}		