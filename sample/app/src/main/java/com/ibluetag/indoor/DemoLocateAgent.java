package com.ibluetag.indoor;

import android.os.Handler;
import android.util.Log;
import com.ibluetag.indoor.sdk.location.LocateAgent;
import com.ibluetag.indoor.sdk.location.Location;
import com.ibluetag.indoor.sdk.location.LocationListener;
import com.ibluetag.loc.uradiosys.PositionAPI;
import com.ibluetag.loc.uradiosys.TagStatus;

import java.util.HashMap;
import java.util.Map;

public class DemoLocateAgent extends LocateAgent {
    private static final String TAG = "DemoLocateAgent";
    private static final int UPDATE_INTERVAL_MS = 2000;

    private Handler mHandler = new Handler();
    private boolean mIsStarted = false;
    private String mTargetMac;

    // 特定应用场景服务器中的地图id与触景地图服务器楼层id映射关系
    private static Map<Long, Long> sFloorMapping = new HashMap<>();
    static {
        sFloorMapping.put(331165345L, 9L);
    }

    // 设置定位服务器
    public void setServer(String serverUrl) {
        Log.v(TAG, "setServer, " + serverUrl);
        PositionAPI.setServer(serverUrl);
    }

    // 设置定位目标MAC
    public void setTarget(String mac) {
        Log.v(TAG, "setTarget, " + mac);
        mTargetMac = mac;
    }

    @Override
    public void requestLocation() {
        updateTagStatus();
    }

    @Override
    public void start() {
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

    private void updateTagStatus() {
        PositionAPI.getTagStatus(mTargetMac, new PositionAPI.TagStatusCallback() {
            @Override
            public void onResult(TagStatus tagStatus) {
                if (tagStatus == null) {
                    Log.w(TAG, "get tag status fail, mac: " + mTargetMac);
                    notifyLocation(LocationListener.LOCATE_UNKNOWN_ERROR, null);
                    return;
                }
                if (sFloorMapping.get(tagStatus.getMapId()) == null) {
                    Log.w(TAG, "floor id not found for map<" + tagStatus.getMapId() + ">");
                    notifyLocation(LocationListener.LOCATE_UNKNOWN_ERROR, null);
                    return;
                }
                Location loc = new Location().setFloor(sFloorMapping.get(tagStatus.getMapId()))
                                             .setX(tagStatus.getX())
                                             .setY(tagStatus.getY());
                Log.v(TAG, "notifyLocation, " + loc);
                notifyLocation(LocationListener.LOCATE_SUCCESS, loc);
            }
        });
    }

    private Runnable mUpdateTagRunnable = new Runnable() {
        @Override
        public void run() {
            updateTagStatus();
            mHandler.postDelayed(mUpdateTagRunnable, UPDATE_INTERVAL_MS);
        }
    };
}
