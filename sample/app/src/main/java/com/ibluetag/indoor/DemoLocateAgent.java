package com.ibluetag.indoor;

import android.graphics.PointF;
import android.os.Handler;
import android.util.Log;
import com.ibluetag.indoor.sdk.location.LocateAgent;
import com.ibluetag.indoor.sdk.location.Location;
import com.ibluetag.indoor.sdk.location.LocationListener;

import java.util.Timer;
import java.util.TimerTask;

// TODO: implemented by locating protocol ('loc.jar')
public class DemoLocateAgent extends LocateAgent {
    private static final String TAG = "DemoLocateAgent";

    private Handler mHandler = new Handler();
    private boolean mIsStarted = false;
    private FakeRoute mFakeRoute = new FakeRoute();

    @Override
    public void requestLocation() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Location loc = new Location().setBuilding(1)
                                             .setFloor(1)
                                             .setX(600)
                                             .setY(600);
                Log.v(TAG, "notifyLocation, " + loc);
                notifyLocation(LocationListener.LOCATE_SUCCESS, loc);
            }
        }, 1000);
    }

    @Override
    public void start() {
        mFakeRoute.start();
        mIsStarted = true;
    }

    @Override
    public void stop() {
        mFakeRoute.stop();
        mIsStarted = false;
    }

    @Override
    public boolean isStarted() {
        return mIsStarted;
    }

    class FakeRoute {
        private Timer mTimer = null;
        private TimerTask mTimerTask = null;
        private boolean mIsRunning = false;
        private int mCounter = 0;
        private PointF mCurPos = new PointF();

        public void start() {
            if (mIsRunning) {
                return;
            }
            if (mTimer == null) {
                mTimer = new Timer();
            }

            if (mTimerTask == null) {
                mCounter = 0;
                mCurPos.x = 230;
                mCurPos.y = 1110;
                mTimerTask = new TimerTask() {
                    @Override
                    public void run() {
                        int step = (mCounter++) % 40;
                        if (step >= 0 && step <= 9) {
                            mCurPos.x += 20;
                        } else if (step >= 10 && step <= 19) {
                            mCurPos.y += 20;
                        } else if (step >= 20 && step <= 29) {
                            mCurPos.x -= 20;
                        } else {
                            mCurPos.y -= 20;
                        }
                        Location loc = new Location().setBuilding(1)
                                                     .setFloor(1)
                                                     .setX(mCurPos.x)
                                                     .setY(mCurPos.y);
                        Log.v(TAG, "notifyLocation, " + loc);
                        notifyLocation(LocationListener.LOCATE_SUCCESS, loc);
                    }
                };
            }
            mTimer.scheduleAtFixedRate(mTimerTask, 0, 1000);
            mIsRunning = true;
        }

        public void stop() {
            if (!mIsRunning) {
                return;
            }
            if (mTimer != null) {
                mTimer.cancel();
                mTimer = null;
            }
            if (mTimerTask != null) {
                mTimerTask.cancel();
                mTimerTask = null;
            }
            mIsRunning = false;
        }
    }
}
