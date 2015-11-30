package com.ibluetag.indoor;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import com.ibluetag.indoor.sdk.location.LocateAgent;
import com.ibluetag.indoor.sdk.location.Location;
import com.ibluetag.indoor.sdk.location.LocationListener;
import com.ibluetag.loc.beacon.api.BeaconLocator;
import com.ibluetag.loc.beacon.model.TargetStatus;
import com.ibluetag.sdk.api.BeaconAPI;
import com.ibluetag.sdk.api.BeaconListener;
import com.ibluetag.sdk.model.SimpleBeacon;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

//基于Beacon定位的LocateAgent（抽象类）
//基于不同的定位技术可以实现不同的LocateAgent，Locategent获取位置，并把位置汇报给地图更新位置点

//每个Locategent都有start/stop/registerLocationListener/unregisterLocationListener
//和requestLocation函数。这些函数可以被外部调用。
//注：LocateAgent的notifyLocation函数通知地图更新位置。
//注：Beacon定位的底层逻辑是通过BeaconLocator实现的。
public class BeaconLocateAgent extends LocateAgent {
    private static final String TAG = "BeaconLocateAgent";
    private static final boolean BLUETOOTH_ALERT_ENABLED = true;

    private Context mContext;
    //BeaconLocator是个对Beacon发现的包装，该类被创建后自动初始化Beacon发现过程（通过底层库实现）
    private BeaconLocator mBeaconLocator;

    private Handler mHandler = new Handler();
    private boolean mIsStarted = false;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static boolean sAvailable = false;
    FileOutputStream mBeaconRecordStream;

    public BeaconLocateAgent(Context context, String uuid) {
        mContext = context;
        //create BeaconLocator
        //BeaconLocator是个对Beacon发现的包装，该类被创建后自动初始化Beacon发现过程（通过底层库实现）
        mBeaconLocator = new BeaconLocator(context, uuid);
    }

    public void destroy() {
        mBeaconLocator.destroy();
    }

    //设置地图服务器URL
    //地图服务器存放地图和Beacon位置，Beacon定位时会查询地图服务器得知定位位置对应的地图
    public void setMapServer(String serverUrl) {
        if (mBeaconLocator == null) {
            Log.w(TAG, "setMapServer, BeaconLocator null...");
            return;
        }
        Log.v(TAG, "setMapServer, " + serverUrl);
        mBeaconLocator.setMapServer(serverUrl);
    }

    //设置底层Beacon一次扫描发现的时间间隔
    public void setBeaconScanInterval(int millis) {
        if (mBeaconLocator == null) {
            Log.w(TAG, "setBeaconScanInterval, BeaconLocator null...");
            return;
        }
        Log.v(TAG, "setBeaconScanInterval, " + millis);
        mBeaconLocator.setBeaconScanInterval(millis);
    }

    public static boolean isAvailable() {
        return sAvailable;
    }

    //获取控制Beacon底层扫描的类
    public BeaconLocator getBeaconLocator() {
        return mBeaconLocator;
    }

    //判断是否当前地图存在Beacon定位功能：
    //一张地图支持wifi定位或者beacon定位，但优先判断是否存在beacon定位
    //地图里面若标记了Beacons位置，则当前地图的定位切换到Beacon定位！
    //参见DemoIndoorActivity的mIndoorMap.getMapProxy().setLocateAgent(mBeaconLocateAgent);
    public static void checkAvailable(long floorId, final StatusListener listener) {
        BeaconLocator.checkAvailable(floorId, new BeaconLocator.Listener() {
            @Override
            public void onResult(boolean available) {
                sAvailable = available;
                listener.onResult(available);
            }
        });
    }

    //异步获取位置
    //通过reportLocation把当前位置，即TargetStatus（含mapid和xy位置）
    @Override
    public void requestLocation() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                reportLocation(mBeaconLocator.requestTargetStatus(), true);
            }
        });
    }

    //启动这个Beacon定位的LocatorAgent
    @Override
    public void start() {
        if (!mIsStarted) {
            Log.v(TAG, "start locate agent");
            if (mBluetoothAdapter == null) {
                Toast.makeText(mContext, R.string.toast_bluetooth_invalid,
                        Toast.LENGTH_SHORT).show();
            } else if (BLUETOOTH_ALERT_ENABLED && !mBluetoothAdapter.isEnabled()) {
                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.beacon_enable_bluetooth_title)
                        .setMessage(R.string.beacon_enable_bluetooth_message)
                        .setPositiveButton(R.string.btn_confirm, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(
                                        BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                mContext.startActivity(intent);
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
            }

            mBeaconLocator.registerListener(mLocationListener);
            mBeaconLocator.start();
            mIsStarted = true;
        }
    }

    //停止Beacon定位
    @Override
    public void stop() {
        if (mIsStarted) {
            Log.v(TAG, "stop locate agent");
            mBeaconLocator.stop();
            mBeaconLocator.unRegisterListener(mLocationListener);
            mIsStarted = false;
        }
    }

    @Override
    public boolean isStarted() {
        return mIsStarted;
    }

    //
    private void reportLocation(final TargetStatus targetStatus, boolean force) {
        if (targetStatus != null) {
            Location loc = new Location().setFloor(targetStatus.getMapId())
                                         .setX(targetStatus.getPosition().x)
                                         .setY(targetStatus.getPosition().y)
                                         .setUnit(Location.Unit.PIXEL);
            Log.v(TAG, "reportLocation, " + loc);

            //通知地图更新定位点的位置，通过执行LocateAgent中注册的回调函数Listener完成
            //注：回调函数通过mIndoorMap.getMapProxy().setLocateAgent(mLocateAgent)注册
            notifyLocation(LocationListener.LOCATE_SUCCESS, loc);
        } else {
            notifyLocation(force ? LocationListener.LOCATE_FORCE_FAILURE :
                    LocationListener.LOCATE_UNKNOWN_ERROR, null);
        }
    }

    private BeaconLocator.TargetListener mLocationListener =
            new BeaconLocator.TargetListener() {

        @Override
        public void onTargetUpdate(final TargetStatus targetStatus) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    reportLocation(targetStatus, false);
                }
            });
        }
    };

    public interface StatusListener {
        public void onResult(boolean available);
    }

    /*
    recording beacons to a file
     */
    //开启iBeacon设备纪录，注册一个回调函数来处理纪录
    public void startBeaconRecord() {
        Log.v(TAG, "startBeaconRecord ");
        if (mBeaconLocator != null) {
            if (mBeaconRecordStream == null) {
                openBeaconRecordFile();//open file
            }
            mBeaconLocator.addBeaconListener(mBeaconRecordListener);
        }
    }

    //停止纪录iBeacon设备
    public void stopBeaconRecord() {
        Log.v(TAG, "stopBeaconRecord ");
        if (mBeaconLocator != null) {
            mBeaconLocator.removeBeaconListener(mBeaconRecordListener);
            if (mBeaconRecordStream != null) {
                closeBeaconRecordFile();
            }
        }
    }

    //底层BeaconAPI扫描到iBeacon后会调用这个回调
    private BeaconListener mBeaconRecordListener = new BeaconListener() {
        @Override
        public void onRefresh(final List<SimpleBeacon> beacons) {
            recordBeacon2File(beacons);
        }
    };

    private void openBeaconRecordFile() {
        String path = Environment.getExternalStorageDirectory().getPath();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String filePath = path + "/imapview_" + timeStamp + ".txt";
        File file = new File(filePath);
        try {
            mBeaconRecordStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            mBeaconRecordStream = null;
        }
    }

    private void closeBeaconRecordFile() {
        if (mBeaconRecordStream != null) {
            try {
                mBeaconRecordStream.close();
            } catch (IOException e) {
                ;
            }
            mBeaconRecordStream = null;
        }
    }

    private void recordBeacon2File(List<SimpleBeacon> beacons) {
        if (mBeaconRecordStream != null) {
            String timeStamp = "T:" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            timeStamp += "\r\n";
            try {
                mBeaconRecordStream.write(timeStamp.getBytes());
                for (SimpleBeacon beacon : beacons) {
                    String line = beacon.toString() + "\r\n";
                    mBeaconRecordStream.write(line.getBytes());
                }
                mBeaconRecordStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "write file error: mBeaconRecordStream");
            }
        }
    }


    /*
    display beacons on BeaconGraph
     */
    Runnable mBeaconGraphRunnable = null;
    public void startBeaconGraph(Runnable runnable) {
        Log.v(TAG, "startBeaconGraph ");
        if (mBeaconLocator != null) {
            mBeaconLocator.addBeaconListener(mBeaconGraphListener);
            mBeaconGraphRunnable = runnable;
        }
    }

    public void stopBeaconGraph() {
        Log.v(TAG, "stopBeaconGraph ");
        if (mBeaconLocator != null) {
            mBeaconLocator.removeBeaconListener(mBeaconGraphListener);
            mBeaconGraphRunnable = null;
        }
    }

    private BeaconListener mBeaconGraphListener = new BeaconListener() {
        @Override
        public void onRefresh(final List<SimpleBeacon> beacons) {
            if (mBeaconGraphRunnable != null) {
                mHandler.post(mBeaconGraphRunnable);
            }
        }
    };

}
