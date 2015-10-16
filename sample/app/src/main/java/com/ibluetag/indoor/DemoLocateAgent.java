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

//基于WIFI定位的LocateAgent
//基于不同的定位技术可以实现不同的LocateAgent，Locategent获取位置，并把位置汇报给地图更新位置点

//每个Locategent都有start/stop/registerLocationListener/unregisterLocationListener
//和requestLocation函数。这些函数可以被外部调用。
//注：LocateAgent的notifyLocation函数通知地图更新位置。
public class DemoLocateAgent extends LocateAgent {
    private static final String TAG = "DemoLocateAgent";
    private static final long DEFAULT_UPDATE_INTERVAL_MS = 2000;

    private Handler mHandler = new Handler();
    private boolean mIsStarted = false;
    private String mTargetMac;
    private List<Listener> mListeners = new ArrayList<Listener>();
    private AreaInfo mLastAreaInfo = null;
    private boolean mIsPushEnabled = false;
    private long mUpdateInterval = DEFAULT_UPDATE_INTERVAL_MS;

    // 设置地图服务器URL
    //注：定位是相对某个地图的，地图来自地图服务器
    public void setMapServer(String serverUrl) {
        Log.v(TAG, "setMapServer, " + serverUrl);
        PositionAPI.setMapServer(serverUrl);
    }

    // 设置WIFI定位引擎服务器URL
    //WIFI的定位位置来自定位引擎服务器，手机从服务器获取位置点
    public void setLocateServer(String urlString) {
        Log.v(TAG, "setLocateServer, " + urlString);
        PositionAPI.setLocateServer(urlString);
        if (mIsStarted) {
            // update tag status at once
            updateTagStatus(false);
        }
    }

    // 设置定位目标MAC
    //为本手机获取位置还是获取别的手机的位置，作为传递给定位引擎服务器的一个参数
    public void setTarget(String mac) {
        Log.v(TAG, "setTarget, " + mac);
        mTargetMac = mac;
    }

    // 设置Beacon服务器
    // 当定位引擎服务器需要手机主动汇报检测到的Beacon时使用，一般不用。
    public void setBeaconServer(String urlString) {
        Log.v(TAG, "setBeaconServer, " + urlString);
        PositionAPI.setBeaconServer(urlString);
    }

    //设置定位位置的更新间隔，默认是2s
    public void setUpdateInterval(long interval) {
        Log.v(TAG, "setUpdateInterval, " + interval);
        if (interval >= 0) {
            mUpdateInterval = interval;
        }
    }

    //把检测到的beacon传递给服务器作辅助，一般不用
    public void reportDevice(Context context, String mac,
                             float degree, List<SimpleBeacon> beacons) {
        PositionAPI.reportBeacon(context, mac, degree, beacons);
    }

    // 设置推送服务器，
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

    //响应推送信息，调用推送信息的回调函数
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
        if (!mIsStarted) {
            // clear info
            Log.v(TAG, "start");
            mLastAreaInfo = null;
            mHandler.post(mUpdateTagRunnable);
            mIsStarted = true;
        }
    }

    @Override
    public void stop() {
        if (mIsStarted) {
            Log.v(TAG, "stop");
            mHandler.removeCallbacks(mUpdateTagRunnable);
            mIsStarted = false;
        }
    }

    @Override
    public boolean isStarted() {
        return mIsStarted;
    }

    //从定位引擎获取位置，并通过notifyLocation更新地图上的位置。
    private void updateTagStatus(final boolean force) {
        PositionAPI.getTagStatus(mTargetMac, new PositionAPI.TagStatusCallback() {
            @Override
            public void onResult(TagStatus tagStatus) {
                if (!mIsStarted) {
                    // ignore callback if agent is stopped
                    // we may receive connection timeout callback from one of inner/outer network
                    return;
                }
                if (tagStatus == null) {
                    Log.w(TAG, "get tag status fail, mac: " + mTargetMac);
                    notifyLocation(force ? LocationListener.LOCATE_FORCE_FAILURE :
                            LocationListener.LOCATE_UNKNOWN_ERROR, null);
                    return;
                }
                if (tagStatus.getMapId() <= 0) {
                    Log.w(TAG, "floor id invalid: " + tagStatus);
                    notifyLocation(force ? LocationListener.LOCATE_FORCE_FAILURE :
                            LocationListener.LOCATE_UNKNOWN_ERROR, null);
                    return;
                }
                Location loc = new Location().setFloor(tagStatus.getMapId())
                                             .setX(tagStatus.getX())
                                             .setY(tagStatus.getY());
                Log.v(TAG, "notifyLocation, " + loc);
                //更新地图的定位位置
                notifyLocation(LocationListener.LOCATE_SUCCESS, loc);

                //如果当前位置有推送信息，就响应这个推送信息
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
            mHandler.postDelayed(mUpdateTagRunnable, mUpdateInterval);
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
