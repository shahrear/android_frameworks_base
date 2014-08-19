/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.util.Log;

import com.android.internal.os.ProcessCpuTracker;

public class LoadAverageService extends Service {
    private View mView;

    private static final class CpuTracker extends ProcessCpuTracker {
        String mLoadText;
        int mLoadWidth;

        private final Paint mPaint;

        CpuTracker(Paint paint) {
            super(false);
            mPaint = paint;
        }

        @Override
        public void onLoadChanged(float load1, float load5, float load15) {
            mLoadText = load1 + " / " + load5 + " / " + load15;
            mLoadWidth = (int)mPaint.measureText(mLoadText);
        }

        @Override
        public int onMeasureProcessName(String name) {
            return (int)mPaint.measureText(name);
        }
    }

    private class LoadView extends View {
        private Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == 1) {
                    mStats.update();
                    updateDisplay();
                    Message m = obtainMessage(1);
                    sendMessageDelayed(m, 1000);
                }
            }
        };

        private final CpuTracker mStats;

        private Paint mLoadPaint;
        private Paint mInfoPaint;
        private Paint mAddedPaint;
        private Paint mRemovedPaint;
        private Paint mShadowPaint;
        private Paint mShadow2Paint;
        private Paint mShadow3Paint;
        private Paint mIrqPaint;
        private Paint mSystemPaint;
        private Paint mUserPaint;
        private float mAscent;
        private int mFH;

        private int mNeededWidth;
        private int mNeededHeight;

        LoadView(Context c) {
            super(c);

            setPadding(4, 4, 4, 4);
            //setBackgroundResource(com.android.internal.R.drawable.load_average_background);

            // Need to scale text size by density...  but we won't do it
            // linearly, because with higher dps it is nice to squeeze the
            // text a bit to fit more of it.  And with lower dps, trying to
            // go much smaller will result in unreadable text.
            int textSize = 10;
            float density = c.getResources().getDisplayMetrics().density;
            if (density < 1) {
                textSize = 9;
            } else {
                textSize = (int)(10*density);
                if (textSize < 10) {
                    textSize = 10;
                }
            }
            mLoadPaint = new Paint();
            mLoadPaint.setAntiAlias(true);
            mLoadPaint.setTextSize(textSize);
            mLoadPaint.setARGB(255, 255, 255, 255);

            mInfoPaint = new Paint();
            mInfoPaint.setAntiAlias(true);
            mInfoPaint.setTextSize(textSize + 5);
            mInfoPaint.setARGB(180, 255, 255, 255);

            mAddedPaint = new Paint();
            mAddedPaint.setAntiAlias(true);
            mAddedPaint.setTextSize(textSize);
            mAddedPaint.setARGB(255, 128, 255, 128);

            mRemovedPaint = new Paint();
            mRemovedPaint.setAntiAlias(true);
            mRemovedPaint.setStrikeThruText(true);
            mRemovedPaint.setTextSize(textSize);
            mRemovedPaint.setARGB(255, 255, 128, 128);

            mShadowPaint = new Paint();
            mShadowPaint.setAntiAlias(true);
            mShadowPaint.setTextSize(textSize);
            //mShadowPaint.setFakeBoldText(true);
            mShadowPaint.setARGB(192, 0, 0, 0);
            mLoadPaint.setShadowLayer(4, 0, 0, 0xff000000);

            mShadow2Paint = new Paint();
            mShadow2Paint.setAntiAlias(true);
            mShadow2Paint.setTextSize(textSize);
            //mShadow2Paint.setFakeBoldText(true);
            mShadow2Paint.setARGB(192, 0, 0, 0);
            mLoadPaint.setShadowLayer(2, 0, 0, 0xff000000);

			mShadow3Paint = new Paint();
            mShadow3Paint.setAntiAlias(true);
            mShadow3Paint.setTextSize(textSize + 5);
            //mShadow2Paint.setFakeBoldText(true);
            mShadow2Paint.setARGB(192, 0, 0, 0);
            mInfoPaint.setShadowLayer(2, 0, 0, 0xff000000);

            mIrqPaint = new Paint();
            mIrqPaint.setARGB(0x80, 0, 0, 0xff);
            mIrqPaint.setShadowLayer(2, 0, 0, 0xff000000);
            mSystemPaint = new Paint();
            mSystemPaint.setARGB(0x80, 0xff, 0, 0);
            mSystemPaint.setShadowLayer(2, 0, 0, 0xff000000);
            mUserPaint = new Paint();
            mUserPaint.setARGB(0x80, 0, 0xff, 0);
            mSystemPaint.setShadowLayer(2, 0, 0, 0xff000000);

            mAscent = mLoadPaint.ascent();
            float descent = mLoadPaint.descent();
            mFH = (int)(descent - mAscent + .5f);

            mStats = new CpuTracker(mLoadPaint);
            mStats.init();
            updateDisplay();
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            mHandler.sendEmptyMessage(1);
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            mHandler.removeMessages(1);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(resolveSize(mNeededWidth, widthMeasureSpec),
                    resolveSize(mNeededHeight, heightMeasureSpec));
        }

		private String[] clock = new String[8];
		private String[] temp = new String[5];
		private int[] core_usage = new int[8];
		private String[] usage = new String[8];
		private float[] data = new float[12];
		boolean check_energy_monitor = true;

        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            final int W = mNeededWidth;
            final int RIGHT = getWidth()-1;

            final CpuTracker stats = mStats;
            final int userTime = stats.getLastUserTime();
            final int systemTime = stats.getLastSystemTime();
            final int iowaitTime = stats.getLastIoWaitTime();
            final int irqTime = stats.getLastIrqTime();
            final int softIrqTime = stats.getLastSoftIrqTime();
            final int idleTime = stats.getLastIdleTime();

            final int totalTime = userTime+systemTime+iowaitTime+irqTime+softIrqTime+idleTime;
            if (totalTime == 0) {
                return;
            }
            int userW = (userTime*W)/totalTime;
            int systemW = (systemTime*W)/totalTime;
            int irqW = ((iowaitTime+irqTime+softIrqTime)*W)/totalTime;

            int x = RIGHT - mPaddingRight;
            int top = mPaddingTop + 2;
            int bottom = mPaddingTop + mFH - 2;

            if (irqW > 0) {
                canvas.drawRect(x-irqW, top, x, bottom, mIrqPaint);
                x -= irqW;
            }
            if (systemW > 0) {
                canvas.drawRect(x-systemW, top, x, bottom, mSystemPaint);
                x -= systemW;
            }
            if (userW > 0) {
                canvas.drawRect(x-userW, top, x, bottom, mUserPaint);
                x -= userW;
            }

            int y = mPaddingTop - (int)mAscent;
            canvas.drawText(stats.mLoadText, RIGHT-mPaddingRight-stats.mLoadWidth-1,
                    y-1, mShadowPaint);
            canvas.drawText(stats.mLoadText, RIGHT-mPaddingRight-stats.mLoadWidth-1,
                    y+1, mShadowPaint);
            canvas.drawText(stats.mLoadText, RIGHT-mPaddingRight-stats.mLoadWidth+1,
                    y-1, mShadow2Paint);
            canvas.drawText(stats.mLoadText, RIGHT-mPaddingRight-stats.mLoadWidth+1,
                    y+1, mShadow2Paint);
            canvas.drawText(stats.mLoadText, RIGHT-mPaddingRight-stats.mLoadWidth,
                    y, mLoadPaint);

			String gpu = InfoUtils.GetGPUCurFreq();
			int pwm_duty = InfoUtils.GetPWMDuty();
			clock = InfoUtils.GetCPUCurFreq();
			if (clock == null)
				Log.e("LoadAverageService", "Not availabled CPU clock");
			temp = InfoUtils.GetTemperature();
			if (temp == null)
				Log.e("LoadAverageService", "Not availabled temperature");
			InfoUtils.GetCPUUsage(core_usage);
			for (int i = 0; i < 8; i++)
				usage[i] = Integer.toString(core_usage[i]);

			if (INA231 >= 0)
				InfoUtils.GetINA231(data);
			else
				Log.e("LoadAverageService", "Not availabled Energy Monitor");
	
			String Fan_Speed = Integer.toString(pwm_duty);
	
			int mFH2 = mFH * 2;

			String str = "";
            if (gpu != null) {
                str = "GPU : " + gpu + "MHz " + temp[4] + "°C";
                canvas.drawText(str, 2, y-1, mShadow3Paint);
                canvas.drawText(str, 2, y+1, mShadow3Paint);
                canvas.drawText(str, 1, y, mInfoPaint);
            }

			str = "CPU1 : " + clock[0] + "MHz, " + usage[0] + "%";
            canvas.drawText(str, 2, y+mFH2-1, mShadow3Paint);
            canvas.drawText(str, 2, y+mFH2+1, mShadow3Paint);
            canvas.drawText(str, 1, y+mFH2, mInfoPaint);

			str = "CPU2 : " + clock[1] + "MHz, " + usage[1] + "%";
            canvas.drawText(str, 2, y+mFH2*2-1, mShadow3Paint);
            canvas.drawText(str, 2, y+mFH2*2+1, mShadow3Paint);
            canvas.drawText(str, 1, y+mFH2*2, mInfoPaint);

			str = "CPU3 : " + clock[2] + "MHz, " + usage[2] + "%";
            canvas.drawText(str, 2, y+mFH2*3+1, mShadow3Paint);
            canvas.drawText(str, 2, y+mFH2*3-1, mShadow3Paint);
            canvas.drawText(str, 1, y+mFH2*3, mInfoPaint);

			str = "CPU4 : " + clock[3] + "MHz, " + usage[3] + "%";
            canvas.drawText(str, 2, y+mFH2*4-1, mShadow3Paint);
            canvas.drawText(str, 2, y+mFH2*4+1, mShadow3Paint);
            canvas.drawText(str, 1, y+mFH2*4, mInfoPaint);

            str = "CPU5 : " + clock[4] + "MHz, " + usage[4] + "% " + temp[0] + "°C";
            canvas.drawText(str, 2, y+mFH2*5-1, mShadow3Paint);
            canvas.drawText(str, 2, y+mFH2*5+1, mShadow3Paint);
            canvas.drawText(str, 1, y+mFH2*5, mInfoPaint);

            str = "CPU6 : " + clock[5] + "MHz, " + usage[5] + "% " + temp[1] + "°C";

            canvas.drawText(str, 2, y+mFH2*6-1, mShadow3Paint);
            canvas.drawText(str, 2, y+mFH2*6+1, mShadow3Paint);
            canvas.drawText(str, 1, y+mFH2*6, mInfoPaint);

            str = "CPU7 : " + clock[6] + "MHz, " + usage[6] + "% " + temp[2] + "°C";
            canvas.drawText(str, 2, y+mFH2*7-1, mShadow3Paint);
            canvas.drawText(str, 2, y+mFH2*7+1, mShadow3Paint);
            canvas.drawText(str, 1, y+mFH2*7, mInfoPaint);

            str = "CPU8 : " + clock[7] + "MHz, " + usage[7] + "% " + temp[3] + "°C";
            canvas.drawText(str, 2, y+mFH2*8-1, mShadow3Paint);
            canvas.drawText(str, 2, y+mFH2*8+1, mShadow3Paint);
            canvas.drawText(str, 1, y+mFH2*8, mInfoPaint);

            /*
			str = "Fan Speed : " + Fan_Speed + "%";
			canvas.drawText(str, 2, y+mFH2*5-1, mShadow3Paint);
			canvas.drawText(str, 2, y+mFH2*5+1, mShadow3Paint);
			canvas.drawText(str, 1, y+mFH2*5, mInfoPaint);
            */

			if (INA231 >= 0) {
				str = "A15 Power : " + data[0] + "V, " + data[1] + "A, " + data[2] + "W";
				canvas.drawText(str, 2, y+mFH2*9-1, mShadow3Paint);
				canvas.drawText(str, 2, y+mFH2*9+1, mShadow3Paint);
				canvas.drawText(str, 1, y+mFH2*9, mInfoPaint);

				str = "A7  Power : " + data[3] + "V, " + data[4] + "A, " + data[5] + "W";
				canvas.drawText(str, 2, y+mFH2*10-1, mShadow3Paint);
				canvas.drawText(str, 2, y+mFH2*10+1, mShadow3Paint);
				canvas.drawText(str, 1, y+mFH2*10, mInfoPaint);

				str = "GPU Power : " + data[6] + "V, " + data[7] + "A, " + data[8] + "W";
				canvas.drawText(str, 2, y+mFH2*11-1, mShadow3Paint);
				canvas.drawText(str, 2, y+mFH2*11+1, mShadow3Paint);
				canvas.drawText(str, 1, y+mFH2*11, mInfoPaint);

				str = "MEM Power : " + data[9] + "V, " + data[10] + "A, " + data[11] + "W";
				canvas.drawText(str, 2, y+mFH2*12-1, mShadow3Paint);
				canvas.drawText(str, 2, y+mFH2*12+1, mShadow3Paint);
				canvas.drawText(str, 1, y+mFH2*12, mInfoPaint);
			} else {
				str = "Energy Monitor not supported";
				canvas.drawText(str, 2, y+mFH2*9-1, mShadow3Paint);
				canvas.drawText(str, 2, y+mFH2*9+1, mShadow3Paint);
				canvas.drawText(str, 1, y+mFH2*9, mInfoPaint);
			}

            int N = stats.countWorkingStats();
            for (int i=0; i<N; i++) {
                CpuTracker.Stats st = stats.getWorkingStats(i);
                y += mFH;
                top += mFH;
                bottom += mFH;

                userW = (st.rel_utime*W)/totalTime;
                systemW = (st.rel_stime*W)/totalTime;
                x = RIGHT - mPaddingRight;
                if (systemW > 0) {
                    canvas.drawRect(x-systemW, top, x, bottom, mSystemPaint);
                    x -= systemW;
                }
                if (userW > 0) {
                    canvas.drawRect(x-userW, top, x, bottom, mUserPaint);
                    x -= userW;
                }

                canvas.drawText(st.name, RIGHT-mPaddingRight-st.nameWidth-1,
                        y-1, mShadowPaint);
                canvas.drawText(st.name, RIGHT-mPaddingRight-st.nameWidth-1,
                        y+1, mShadowPaint);
                canvas.drawText(st.name, RIGHT-mPaddingRight-st.nameWidth+1,
                        y-1, mShadow2Paint);
                canvas.drawText(st.name, RIGHT-mPaddingRight-st.nameWidth+1,
                        y+1, mShadow2Paint);
                Paint p = mLoadPaint;
                if (st.added) p = mAddedPaint;
                if (st.removed) p = mRemovedPaint;
                canvas.drawText(st.name, RIGHT-mPaddingRight-st.nameWidth, y, p);
            }
        }

        void updateDisplay() {
            final CpuTracker stats = mStats;
            final int NW = stats.countWorkingStats();

            int maxWidth = stats.mLoadWidth;
            for (int i=0; i<NW; i++) {
                CpuTracker.Stats st = stats.getWorkingStats(i);
                if (st.nameWidth > maxWidth) {
                    maxWidth = st.nameWidth;
                }
            }

            int neededWidth = mPaddingLeft + mPaddingRight + maxWidth;
            int neededHeight = mPaddingTop + mPaddingBottom + (mFH * 2 * 9 *(1+NW));
            if (neededWidth != mNeededWidth || neededHeight != mNeededHeight) {
                mNeededWidth = neededWidth;
                mNeededHeight = neededHeight;
                requestLayout();
				//codewalker
				invalidate();
            } else {
                invalidate();
            }
        }
    }

	private int INA231 = -1;

    @Override
    public void onCreate() {
        super.onCreate();
		INA231 = InfoUtils.OpenINA231();
        mView = new LoadView(this);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.END | Gravity.TOP;
        params.setTitle("Load Average");
        WindowManager wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        wm.addView(mView, params);
    }

    @Override
    public void onDestroy() {
		InfoUtils.CloseINA231();
        super.onDestroy();
        ((WindowManager)getSystemService(WINDOW_SERVICE)).removeView(mView);
        mView = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
