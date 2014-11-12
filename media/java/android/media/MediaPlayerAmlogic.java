/*
 * AMLOGIC Media Player.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the named License,
 * or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA
 *
 * Author:  Wang Jian <jian.wang@amlogic.com>
 *
 */
package android.media;
//package com.meson.videoplayer;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.lang.Thread.State;
/**
 * Created by wangjian on 2014/4/17.
 */
public class MediaPlayerAmlogic extends MediaPlayer {
    private static final String LOGTAG = "MediaPlayerAmlogic";
    private static final int FF_PLAY_TIME = 5000;
    private static final int FB_PLAY_TIME = 5000;
    private static final int BASE_SLEEP_TIME = 500;
    private static final int ON_FF_COMPLETION = 1;
    private int mStep = 0;
    private boolean mIsFF = true;
    private boolean mStopFast = true;
    private int mPos = -1;
    private Thread mThread = null;
    private OnCompletionListener mOnCompletionListener = null;
    private OnSeekCompleteListener mOnSeekCompleteListener =null;
    private OnErrorListener mOnErrorListener = null;

    public  void fastForward(int step) {
        Log.i(LOGTAG, "fastForward:"+step);
        synchronized (this){
            String playerTypeStr = getStringParameter(MediaPlayer.KEY_PARAMETER_AML_PLAYER_TYPE_STR);
            if((playerTypeStr != null) && (playerTypeStr.equals("AMLOGIC_PLAYER"))) {
                String str = Integer.toString(step);
                StringBuilder builder = new StringBuilder();
                builder.append("forward:"+str);
                Log.i(LOGTAG,"[HW]"+builder.toString());
                setParameter(MediaPlayer.KEY_PARAMETER_AML_PLAYER_TRICKPLAY_FORWARD,builder.toString());
                return;
            }
            mStep = step;
            mIsFF = true;
            mStopFast = false;
            if(mThread == null) {
                mThread = new Thread(runnable);
                mThread.start();
            } else {
                if(mThread.getState() == State.TERMINATED) {
                    mThread = new Thread(runnable);
                    mThread.start();
                }
            }

        }
    }

    public  void fastBackward(int step) {
        Log.i(LOGTAG, "fastBackward:"+step);
        synchronized (this){
            String playerTypeStr = getStringParameter(MediaPlayer.KEY_PARAMETER_AML_PLAYER_TYPE_STR);
            if((playerTypeStr != null) && (playerTypeStr.equals("AMLOGIC_PLAYER"))) {
                String str = Integer.toString(step);
                StringBuilder builder = new StringBuilder();
                builder.append("backward:"+str);
                Log.i(LOGTAG,"[HW]"+builder.toString());
                setParameter(MediaPlayer.KEY_PARAMETER_AML_PLAYER_TRICKPLAY_BACKWARD,builder.toString());
                return;
            }
            mStep = step;
            mIsFF = false;
            mStopFast = false;
            if(mThread == null) {
                mThread = new Thread(runnable);
                mThread.start();
            } else {
                if(mThread.getState() == State.TERMINATED) {
                    mThread = new Thread(runnable);
                    mThread.start();
                }
            }

        }
    }

    public  boolean isPlaying() {
        if(!mStopFast) {
            return true;
        }
        return super.isPlaying();
    }

    public void reset() {
        mStopFast = true;
        super.reset();
    }

    public void start() {
        mStopFast = true;
        super.start();
    }

    public void pause() {
        mStopFast = true;
        super.pause();
    }

    public void stop() {
        mStopFast = true;
        super.stop();
    }

    public void setOnSeekCompleteListener(OnSeekCompleteListener listener)
    {
        mOnSeekCompleteListener = listener;
        super.setOnSeekCompleteListener(mMediaPlayerSeekCompleteListener);
    }

    public void setOnCompletionListener(OnCompletionListener listener)
    {
        mOnCompletionListener = listener;
        super.setOnCompletionListener(mMediaPlayerCompletionListener);
    }

    public void setOnErrorListener(OnErrorListener listener)
    {
        mOnErrorListener = listener;
        super.setOnErrorListener(mMediaPlayerErrorListener);
    }



    private void superPause() {
        super.pause();
    }

    private void superStart() {
        super.start();
    }

    private void OnFFCompletion() {
        if(mOnCompletionListener != null) {
            Log.i(LOGTAG, "mOnCompletionListener.onCompletion");
            mOnCompletionListener.onCompletion(this);
        }
    }

    private Runnable runnable = new Runnable() {
        @Override
            public void run() {
                int pos;
                int duration = getDuration ();
                int sleepTime = BASE_SLEEP_TIME;
                int seekPos = 0;
                superPause();
                while(!mStopFast) {
                    if(mStep <  1) {
                        mStopFast = true;
                        superStart();
                        break;
                    }

                    pos = getCurrentPosition();
                    //Log.i(LOGTAG, "duration:"+ duration+"///pos:"+pos);
                    if(pos == 0) {
                        mStopFast = true;
                        superStart();
                        break;
                    }
                    if( pos == duration || pos == mPos) {
                        stop();
                        Message newMsg = Message.obtain();
                        newMsg.what = ON_FF_COMPLETION;
                        mMainHandler.sendMessage(newMsg);
                        break;
                    }
                    mPos = pos;
                    //seek
                    //int time1 = -1;
                    //int time2;
                    if(mIsFF) {
                        int jumpTime =  mStep * FF_PLAY_TIME;
                        int baseTime = 0;
                        if(mPos < seekPos + sleepTime) {
                            baseTime = seekPos + sleepTime;
                        } else {
                            baseTime = mPos;
                        }
                        seekPos = (baseTime + jumpTime) > duration ? duration : (baseTime + jumpTime);
                        //Log.i(LOGTAG, "seekTo:"+ seekPos);
                        seekTo(seekPos);
                        //time1 = getCurrentPosition();
                        //Log.i(LOGTAG, "111111111:"+ time1);
                    }else {
                        int jumpTime = mStep * FB_PLAY_TIME;
                        seekPos = (mPos - jumpTime)< 0 ? 0 : (mPos - jumpTime);
                        //Log.i(LOGTAG, "seekTo:"+ seekPos);
                        seekTo(seekPos);
                    }

                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    /*
                       for(int i = 0;i < 50 ;i++) {
                       try {
                       Thread.sleep(100);
                       } catch (InterruptedException e) {
                       Thread.currentThread().interrupt();
                       }
                       time2 = getCurrentPosition();
                       if(time2 != time1){
                       Log.i(LOGTAG, "-------i:"+ i);
                       Log.i(LOGTAG, "=======:"+ (time2 - seekPos));
                       break;
                       }
                       }
                     */

                }
            }
    };
    private MediaPlayer.OnSeekCompleteListener mMediaPlayerSeekCompleteListener = 
        new MediaPlayer.OnSeekCompleteListener() {
            public void onSeekComplete(MediaPlayer mp) {
                if(mStopFast) {
                    if(mOnSeekCompleteListener != null) {
                        mOnSeekCompleteListener.onSeekComplete(mp);
                    }
                }
            }
        };

    private MediaPlayer.OnCompletionListener mMediaPlayerCompletionListener = 
        new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                if(!mStopFast) {
                    mStopFast = true;
                }
                if(mOnCompletionListener != null) {
                    mOnCompletionListener.onCompletion(mp);
                }
            }
        };

    private MediaPlayer.OnErrorListener mMediaPlayerErrorListener =
        new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
                if(!mStopFast) {
                    mStopFast = true;
                }
                if(mOnErrorListener != null) {
                    return mOnErrorListener.onError( mp, what, extra);
                }
                return true;
            }
        };
    private Handler mMainHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ON_FF_COMPLETION:
                    OnFFCompletion();
                    break;
            }
        }
    };
}

