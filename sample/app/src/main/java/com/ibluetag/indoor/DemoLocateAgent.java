package com.ibluetag.indoor;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import com.ibluetag.indoor.sdk.MapProxy;
import com.ibluetag.indoor.sdk.location.LocateAgent;
import com.ibluetag.indoor.sdk.location.Location;
import com.ibluetag.indoor.sdk.location.LocationListener;
import com.ibluetag.loc.uradiosys.api.PositionAPI;
import com.ibluetag.loc.uradiosys.model.AreaInfo;
import com.ibluetag.loc.uradiosys.model.TagStatus;
import com.ibluetag.sdk.model.SimpleBeacon;

import java.util.ArrayList;
import java.util.List;

public class DemoLocateAgent extends LocateAgent {
    private static final String TAG = "DemoLocateAgent";
    private static final int UPDATE_INTERVAL_MS = 2000;

    private Handler mHandler = new Handler();
    private boolean mIsStarted = false;
    private String mTargetMac;
    private List<Listener> mListeners = new ArrayList<Listener>();
    private AreaInfo mLastAreaInfo = null;
    private boolean mIsPushEnabled = false;

    // 设置地图服务器
    public void setMapServer(String serverUrl) {
        Log.v(TAG, "setMapServer, " + serverUrl);
        PositionAPI.setMapServer(serverUrl);
    }

    // 设置定位服务器
    public void setLocateServer(String urlString) {
        Log.v(TAG, "setLocateServer, " + urlString);
        PositionAPI.setLocateServer(urlString);
    }

    // 设置定位目标MAC
    public void setTarget(String mac) {
        Log.v(TAG, "setTarget, " + mac);
        mTargetMac = mac;
    }

    // 设置Beacon服务器
    public void setBeaconServer(String urlString) {
        Log.v(TAG, "setBeaconServer, " + urlString);
        PositionAPI.setBeaconServer(urlString);
    }

    public void reportDevice(Context context, String mac,
                             float degree, List<SimpleBeacon> beacons) {
        PositionAPI.reportBeacon(context, mac, degree, beacons);
    }

    // 设置推送服务器
    public void setPushServer(String serverUrl) {
        Log.v(TAG, "setPushServer, " + serverUrl);
        PositionAPI.setPushServer(serverUrl);
    }

    // 推送开关
    public void enablePush(boolean enable) {
        mIsPushEnabled = enable;
    }

    public void registerListener(Listener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public void unRegisterListener(Listener listener) {
        mListeners.remove(listener);
    }

    private void notifyPushListener(AreaInfo areaInfo) {
        for (Listener listener : mListeners) {
            listener.onEnterInfoArea(areaInfo);
        }
    }

    @Override
    public void requestLocation() {
        // clear info
        mLastAreaInfo = null;
        updateTagStatus(true);
        for (Listener listener : mListeners) {
            listener.onRequestLocation();
        }
    }

    @Override
    public void start() {
        // clear info
        mLastAreaInfo = null;
        mHandler.postDelayed(mUpdateTagRunnable, UPDATE_INTERVAL_MS);
        mIsStarted = true;
    }

    @Override
    public void stop() {
        mHandler.removeCallbacks(mUpdateTagRunnable);
        mIsStarted = false;
    }

    @Override
    public boolean isStarted() {
        return mIsStarted;
    }

    private void updateTagStatus(final boolean force) {
        PositionAPI.getTagStatus(mTargetMac, new PositionAPI.TagStatusCallback() {
            @Override
            public void onResult(TagStatus tagStatus) {
                if (tagStatus == null) {
                    Log.w(TAG, "get tag status fail, mac: " + mTargetMac);
                    notifyLocation(force ? LocationListener.LOCATE_FORCE_FAILURE :
                            LocationListener.LOCATE_UNKNOWN_ERROR, null);
                    return;
                }
                if (tagStatus.getMapId() < 0) {
                    Log.w(TAG, "floor id invalid: " + tagStatus);
                    notifyLocation(force ? LocationListener.LOCATE_FORCE_FAILURE :
                            LocationListener.LOCATE_UNKNOWN_ERROR, null);
                    return;
                }
                Location loc = new Location().setFloor(tagStatus.getMapId())
                                             .setX(tagStatus.getX())
                                             .setY(tagStatus.getY());
                Log.v(TAG, "notifyLocation, " + loc);
                notifyLocation(LocationListener.LOCATE_SUCCESS, loc);
                if (mIsPushEnabled && tagStatus.isEnterInfoArea()) {
                    PositionAPI.getAreaInfo(tagStatus, new PositionAPI.AreaInfoCallback() {
                        @Override
                        public void onResult(AreaInfo areaInfo) {
                            Log.v(TAG, "getAreaInfo, " + areaInfo);
                            if (areaInfo == null) {
                                notifyPushListener(null);
                            } else if (areaInfo.getMessage() == null &&
                                    areaInfo.getImage() == null) {
                                notifyPushListener(null);
                            } else if (!areaInfo.equals(mLastAreaInfo)) {
                                notifyPushListener(areaInfo);
                            }
                            mLastAreaInfo = areaInfo;
                        }
                    });
                } else {
                    notifyPushListener(null);
                }
            }
        });
    }

    private Runnable mUpdateTagRunnable = new Runnable() {
        @Override
        public void run() {
            updateTagStatus(false);
            mHandler.postDelayed(mUpdateTagRunnable, UPDATE_INTERVAL_MS);
        }
    };

    public void notifyFakeLocation(long floorId, float x, float y) {
        // test purpose
        Location loc = new Location().setFloor(floorId).setX(x).setY(y);
        Log.v(TAG, "notifyFakeLocation, " + loc);
        notifyLocation(LocationListener.LOCATE_SUCCESS, loc);
    }

    public interface Listener {
        public void onEnterInfoArea(AreaInfo areaInfo);
        public void onRequestLocation();
    }
}
