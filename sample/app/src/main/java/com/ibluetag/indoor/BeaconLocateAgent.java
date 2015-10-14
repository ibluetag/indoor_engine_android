package com.ibluetag.indoor;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import com.ibluetag.indoor.sdk.location.LocateAgent;
import com.ibluetag.indoor.sdk.location.Location;
import com.ibluetag.indoor.sdk.location.LocationListener;
import com.ibluetag.loc.beacon.api.BeaconLocator;
import com.ibluetag.loc.beacon.model.TargetStatus;

public class BeaconLocateAgent extends LocateAgent {
    private static final String TAG = "BeaconLocateAgent";
    private static final boolean BLUETOOTH_ALERT_ENABLED = true;

    private Context mContext;
    private BeaconLocator mBeaconLocator;
    private Handler mHandler = new Handler();
    private boolean mIsStarted = false;
    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static boolean sAvailable = false;

    public BeaconLocateAgent(Context context, String uuid) {
        mContext = context;
        mBeaconLocator = new BeaconLocator(context, uuid);
    }

    public void destroy() {
        mBeaconLocator.destroy();
    }

    public void setMapServer(String serverUrl) {
        if (mBeaconLocator == null) {
            Log.w(TAG, "setMapServer, BeaconLocator null...");
            return;
        }
        Log.v(TAG, "setMapServer, " + serverUrl);
        mBeaconLocator.setMapServer(serverUrl);
    }

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

    public static void checkAvailable(long floorId, final StatusListener listener) {
        BeaconLocator.checkAvailable(floorId, new BeaconLocator.Listener() {
            @Override
            public void onResult(boolean available) {
                sAvailable = available;
                listener.onResult(available);
            }
        });
    }

    @Override
    public void requestLocation() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                reportLocation(mBeaconLocator.requestTargetStatus(), true);
            }
        });
    }

    @Override
    public void start() {
        if (!mIsStarted) {
            Log.v(TAG, "start");
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

    @Override
    public void stop() {
        if (mIsStarted) {
            Log.v(TAG, "stop");
            mBeaconLocator.stop();
            mBeaconLocator.unRegisterListener(mLocationListener);
            mIsStarted = false;
        }
    }

    @Override
    public boolean isStarted() {
        return mIsStarted;
    }

    private void reportLocation(final TargetStatus targetStatus, boolean force) {
        if (targetStatus != null) {
            Location loc = new Location().setFloor(targetStatus.getMapId())
                                         .setX(targetStatus.getPosition().x)
                                         .setY(targetStatus.getPosition().y)
                                         .setUnit(Location.Unit.PIXEL);
            Log.v(TAG, "reportLocation, " + loc);
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
}
