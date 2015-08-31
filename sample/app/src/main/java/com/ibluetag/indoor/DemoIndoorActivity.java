package com.ibluetag.indoor;

import android.app.Activity;
import android.content.*;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.ibluetag.indoor.sdk.model.BitmapOverlay;
import com.ibluetag.indoor.sdk.model.Building;
import com.ibluetag.indoor.sdk.IndoorMapView;
import com.ibluetag.indoor.sdk.MapProxy;
import com.ibluetag.indoor.sdk.model.InfoWindow;
import com.ibluetag.indoor.sdk.model.RouteInfo;
import com.ibluetag.loc.uradiosys.model.AreaInfo;
import com.ibluetag.sdk.api.BeaconAPI;
import com.ibluetag.sdk.api.BeaconListener;
import com.ibluetag.sdk.model.SimpleBeacon;
import com.squareup.picasso.Picasso;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DemoIndoorActivity extends Activity {
    private static final String TAG = "DemoIndoorActivity";
    public static final boolean DUMP_WIFI_SCAN_RESULT = false;

    private TextView mTitle;
    private ImageButton mSearchBtn;
    private View mSearchInputLayout;
    private EditText mSearchInputEdit;
    private ImageButton mSearchInputClearBtn;
    private Button mSearchCancelBtn;

    private View mInfoLayout;
    private ImageButton mInfoCloseBtn;
    private TextView mInfoMessage;
    private ImageView mInfoImage;
    private Button mInfoDetailBtn;

    private IndoorMapView mIndoorMap;
    private Building mCurrentBuilding;
    private long mCurrentFloorId = 1;
    private BitmapOverlay mBitmapOverlay1;
    private BitmapOverlay mBitmapOverlay2;
    private DemoLocateAgent mLocateAgent = new DemoLocateAgent();
    private String mMapServerUrl;
    private long mMapSubjectId;
    private String mTargetMac;
    private WifiManager mWifiManager;
    private Handler mHandler = new Handler();
    private int mWiFiScanInterval = -1;
    private boolean mIsWifiScanning = false;
    private AreaInfo mCurrentAreaInfo;
    private BeaconAPI mBeaconAPI;
    private int mBeaconInterval = -1;
    private boolean mIsToastNotInBuildingRequired = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_indoor_map);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        setupTitleBar();
        setupInfoBar();
        setupIndoorMap();
        setupOverlay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyBeaconScan();
        stopWifiScan();
        mLocateAgent.unRegisterListener(mPushListener);
        if (mLocateAgent.isStarted()) {
            mLocateAgent.stop();
        }
        mBitmapOverlay1.destroy();
        mBitmapOverlay2.destroy();
        mIndoorMap.getMapProxy().destroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isNetworkAvailable(true)) {
            reloadMap();
        }
    }

    private void reloadMap() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (sp.getBoolean(getString(R.string.key_clear_cache), false)) {
            Log.v(TAG, "clear cache...");
            mIndoorMap.getMapProxy().clearCache();
            sp.edit().putBoolean(getString(R.string.key_clear_cache), false).commit();
        }

        mMapServerUrl = sp.getString(getString(R.string.key_map_server),
                getString(R.string.default_map_server));
        mMapSubjectId = Long.parseLong(sp.getString(getString(R.string.key_map_subject_id),
                getString(R.string.default_map_subject_id)));
        mLocateAgent.setMapServer(mMapServerUrl);
        mLocateAgent.enablePush(sp.getBoolean(getString(R.string.key_push_enable), false));
        mLocateAgent.setPushServer(sp.getString(getString(R.string.key_push_server),
                getString(R.string.default_push_server)));

        if (sp.getBoolean(getString(R.string.key_locate_with_phone), false)) {
            startWifiScan();
        } else {
            stopWifiScan();
        }

        if (sp.getBoolean(getString(R.string.key_locate_beacon_discovery), false)) {
            mBeaconInterval = Integer.parseInt(
                    sp.getString(getString(R.string.key_locate_beacon_scan_interval),
                    getString(R.string.default_locate_beacon_scan_interval)));
            enableBeaconScan(true);
        } else {
            enableBeaconScan(false);
        }
        mTargetMac = sp.getString(getString(R.string.key_locate_target),
                getString(R.string.default_locate_target));
        mLocateAgent.setTarget(mTargetMac);

        mIndoorMap.getMapProxy().setRouteAttachDistance(Integer.parseInt(sp.getString(
                getString(R.string.key_route_attach_threshold),
                getString(R.string.default_route_attach_threshold))));
        mIndoorMap.getMapProxy().setRouteDeviateDistance(Integer.parseInt(sp.getString(
                getString(R.string.key_route_deviate_threshold),
                getString(R.string.default_route_deviate_threshold))));
        mIndoorMap.getMapProxy().setRouteRule(Integer.parseInt(sp.getString(
                getString(R.string.key_route_rule),
                getString(R.string.default_route_rule))));
        mIndoorMap.getMapProxy().enableSmoothRoute(
                sp.getBoolean(getString(R.string.key_smooth_route), false));

        String loadMode = sp.getString(getString(R.string.key_map_load_mode),
                getString(R.string.default_map_load_mode));
        String initialFloorId = sp.getString(getString(R.string.key_map_load_initial_floor_id),
                getString(R.string.default_map_load_initial_floor_id));
        String initialLabel = sp.getString(getString(R.string.key_map_load_initial_label),
                getString(R.string.default_map_load_initial_label));

        Log.v(TAG, "reloadMap, map server: " + mMapServerUrl +
                ", id: " + mMapSubjectId +
                ", mode: " + loadMode +
                ", initial floor: " + initialFloorId +
                ", initial label: " + initialLabel);
        mIsToastNotInBuildingRequired = true;
        // 设置地图服务器
        mIndoorMap.getMapProxy().initServer(mMapServerUrl);
        if (loadMode.equals(getString(R.string.value_map_load_mode_subject))) {
            // 通过地图主体ID加载
            mIndoorMap.getMapProxy().load(mMapSubjectId);
        } else if (loadMode.equals(getString(R.string.value_map_load_mode_floor))) {
            // 通过地图主体ID及楼层ID加载
            mIndoorMap.getMapProxy().loadMapWithFloor(mMapSubjectId, Long.valueOf(initialFloorId));
        } else if (loadMode.equals(getString(R.string.value_map_load_mode_booth_select))) {
            // 通过地图主体ID，楼层ID及POI标签加载，加载后选中POI
            mIndoorMap.getMapProxy().loadMapWithPoiSelected(
                    mMapSubjectId, Long.valueOf(initialFloorId), initialLabel);
        } else if (loadMode.equals(getString(R.string.value_map_load_mode_booth_route))) {
            // 通过地图主体ID，楼层ID及POI标签加载，加载后导航至POI
            mIndoorMap.getMapProxy().loadMapWithPoiRouting(
                    mMapSubjectId, Long.valueOf(initialFloorId), initialLabel);
        }

    }

    private void setupTitleBar() {
        // show virtual menu key since action bar is hidden by theme
        // NOTE: not required for formal app
        try {
            getWindow().addFlags(
                    WindowManager.LayoutParams.class.getField("FLAG_NEEDS_MENU_KEY").getInt(null));
        } catch (NoSuchFieldException e) {
            // Ignore since this field won't exist in most versions of Android
        } catch (Exception e) {
            Log.w(TAG, e.toString());
        }

        mTitle = (TextView) findViewById(R.id.title);
        mSearchInputLayout = findViewById(R.id.search_input_layout);
        mSearchInputClearBtn = (ImageButton) findViewById(R.id.search_input_clear);
        mSearchInputClearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearchInputEdit.setText("");
            }
        });

        mSearchBtn = (ImageButton) findViewById(R.id.search_btn);
        mSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mLocateAgent.notifyFakeLocation(9, 2, 6);
                mTitle.setVisibility(View.GONE);
                mSearchBtn.setVisibility(View.GONE);
                mSearchInputLayout.setVisibility(View.VISIBLE);
                mSearchCancelBtn.setVisibility(View.VISIBLE);
            }
        });
        mSearchBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                openOptionsMenu();
                return true;
            }
        });

        mSearchCancelBtn = (Button) findViewById(R.id.venue_map_search_cancel_btn);
        mSearchCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mLocateAgent.notifyFakeLocation(9, 6, 7);
                mIndoorMap.getMapProxy().clearSearch();
                mSearchInputLayout.setVisibility(View.GONE);
                mSearchCancelBtn.setVisibility(View.GONE);
                mTitle.setVisibility(View.VISIBLE);
                mSearchBtn.setVisibility(View.VISIBLE);
            }
        });

        mSearchInputEdit = (EditText) findViewById(R.id.search_input_edit);
        mSearchInputEdit.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER &&
                        event.getAction() == KeyEvent.ACTION_UP) {
                    ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                            .hideSoftInputFromWindow(mSearchInputEdit.getWindowToken(),
                                    InputMethodManager.HIDE_NOT_ALWAYS);
                    int result = mIndoorMap.getMapProxy().search(
                            mSearchInputEdit.getText().toString());
                    if (result > 0) {
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.toast_map_search_success, result),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(),
                                R.string.toast_map_search_failure,
                                Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                return false;
            }
        });
        mSearchInputEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                mSearchInputClearBtn.setVisibility(editable.length() > 0 ?
                        View.VISIBLE : View.GONE);
            }
        });
    }

    private void setupIndoorMap() {
        mLocateAgent.registerListener(mPushListener);
        mIndoorMap = (IndoorMapView) findViewById(R.id.indoor_map);
        // 设置地图加载监听
        mIndoorMap.getMapProxy().setMapListener(new MapProxy.MapListener() {
            @Override
            public void onMapLoaded(boolean success) {
            }

            @Override
            public void onMapLoadPoiLabelNotFound() {
                Toast.makeText(getApplicationContext(),
                        R.string.toast_map_load_poi_label_not_found,
                        Toast.LENGTH_SHORT).show();
            }
        });

        // 设置建筑物切换监听
        mIndoorMap.getMapProxy().setBuildingListener(new MapProxy.BuildingListener() {
            @Override
            public void onBuildingSwitch(Building building) {
                mCurrentBuilding = building;
                if (building.getName() != null && !building.getName().isEmpty()) {
                    mTitle.setText(building.getName());
                }
            }
        });
        // 设置楼层切换监听
        mIndoorMap.getMapProxy().setFloorListener(new MapProxy.FloorListener() {
            @Override
            public void onFloorSwitch(long floorId) {
                mCurrentFloorId = floorId;
                String locateServer = mIndoorMap.getMapProxy().getLocateServer(floorId);
                Log.v(TAG, "locateServer: " + locateServer);
                if (locateServer != null && !locateServer.isEmpty()) {
                    mLocateAgent.setLocateServer(locateServer);
                } else {
                    Log.w(TAG, "no locate server from map info");
                    mLocateAgent.setLocateServer(PreferenceManager.getDefaultSharedPreferences(
                            getApplicationContext()).getString(
                            getString(R.string.key_locate_server),
                            getString(R.string.default_locate_server)));
                }

                String beaconServer = mIndoorMap.getMapProxy().getBeaconServer(floorId);
                Log.v(TAG, "beaconServer: " + beaconServer);
                if (beaconServer != null && !beaconServer.isEmpty()) {
                    mLocateAgent.setBeaconServer(mIndoorMap.getMapProxy().getBeaconServer(floorId));
                } else {
                    Log.w(TAG, "no beacon server from map info");
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(
                            getApplicationContext());
                    mLocateAgent.setBeaconServer(
                            sp.getString(getString(R.string.key_locate_beacon_server),
                                    getString(R.string.default_locate_beacon_server)) + ":" +
                            Integer.parseInt(sp.getString(getString(R.string.key_locate_beacon_port),
                            getString(R.string.default_locate_beacon_port))));
                }
                Log.v(TAG, "floor: " + floorId +
                        ", proportion: " + mIndoorMap.getMapProxy().getProportion());
            }
        });
        // 设置导航监听
        mIndoorMap.getMapProxy().setRouteListener(new MapProxy.RouteListener() {
            @Override
            public void onRouteResult(RouteInfo[] routeInfos) {
            }

            @Override
            public void onRouteOutOfPath() {
                Toast.makeText(getApplicationContext(), R.string.toast_route_out_of_path,
                        Toast.LENGTH_LONG).show();
            }
        });
        // 设置定位监听
        mIndoorMap.getMapProxy().setLocateListener(new MapProxy.LocateListener() {
            @Override
            public void onLocateNotInBuilding() {
                if (mIsToastNotInBuildingRequired) {
                    Toast.makeText(getApplicationContext(), R.string.toast_locate_not_in_building,
                            Toast.LENGTH_LONG).show();
                    mIsToastNotInBuildingRequired = false;
                }
            }
        });
        // 设置定位代理
        mIndoorMap.getMapProxy().setLocateAgent(mLocateAgent);
    }

    private void setupOverlay() {
        mBitmapOverlay1 = new BitmapOverlay()
                .bitmap(BitmapFactory.decodeResource(getResources(), R.drawable.overlay_bitmap1))
                .position(500, 500);
        mBitmapOverlay2 = new BitmapOverlay()
                .bitmap(BitmapFactory.decodeResource(getResources(), R.drawable.overlay_bitmap2))
                .position(700, 300);
    }

    private void setupInfoBar() {
        mInfoLayout = findViewById(R.id.info_layout);
        mInfoCloseBtn = (ImageButton) findViewById(R.id.info_close);
        mInfoMessage = (TextView) findViewById(R.id.info_message);
        mInfoImage = (ImageView) findViewById(R.id.info_image);
        mInfoDetailBtn = (Button) findViewById(R.id.info_detail);
        mInfoCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mInfoLayout.setVisibility(View.GONE);
            }
        });
        mInfoDetailBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentAreaInfo != null &&
                        mCurrentAreaInfo.getLink() != null &&
                        !mCurrentAreaInfo.getLink().isEmpty()) {
                    Intent intent = new Intent(getApplicationContext(), DemoWebActivity.class);
                    intent.putExtra(DemoWebActivity.EXTRA_WEB_URL, mCurrentAreaInfo.getLink());
                    intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                    startActivity(intent);
                } else {
                    Log.w(TAG, "no link for current area..");
                }
            }
        });
    }

    private void updateInfoBar() {
        if (mCurrentAreaInfo == null) {
            mInfoLayout.setVisibility(View.GONE);
            return;
        }
        mInfoMessage.setText(mCurrentAreaInfo.getMessage());
        Picasso.with(getApplicationContext())
               .load(mCurrentAreaInfo.getImage())
               .placeholder(R.drawable.placeholder_img)
               .into(mInfoImage);
        mInfoLayout.setVisibility(View.VISIBLE);
    }

    private DemoLocateAgent.Listener mPushListener = new DemoLocateAgent.Listener() {
        @Override
        public void onEnterInfoArea(AreaInfo areaInfo) {
            mCurrentAreaInfo = areaInfo;
            updateInfoBar();
        }

        @Override
        public void onRequestLocation() {
            mIsToastNotInBuildingRequired = true;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(getApplicationContext(), DemoPreferenceActivity.class));
                return true;
            case R.id.action_add_overlay:
                mBitmapOverlay1.attach(mCurrentBuilding.getId(), mCurrentFloorId);
                mBitmapOverlay2.attach(mCurrentBuilding.getId(), mCurrentFloorId);
                View infoWindowLayout  = getLayoutInflater().inflate(R.layout.layout_info_window,
                        null);
                View infoWindowView = infoWindowLayout.findViewById(R.id.info_window);
                infoWindowView.findViewById(R.id.info_btn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mIndoorMap.getMapProxy().removeInfoWindow();
                    }
                });
                ((TextView)infoWindowView.findViewById(R.id.info_text)).setText(
                        R.string.info_window_text);
                InfoWindow window = new InfoWindow(infoWindowView)
                        .marker(((BitmapDrawable)getResources().getDrawable(
                                R.drawable.info_window_marker)).getBitmap())
                        .position(new PointF(300, 300))
                        .offset(-380, -280);
                mIndoorMap.getMapProxy().showInfoWindow(window);
                return true;
            case R.id.action_remove_overlay:
                mBitmapOverlay1.detach();
                mBitmapOverlay2.detach();
                mIndoorMap.getMapProxy().removeInfoWindow();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isNetworkAvailable(boolean showToast) {
        boolean available = isNetworkAvailable();
        if (showToast && !available) {
            Toast.makeText(this, R.string.toast_network_unavailable,
                    Toast.LENGTH_SHORT).show();
        }
        return available;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivity = (ConnectivityManager)getSystemService(
                Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void startWifiScan() {
        if (mIsWifiScanning) {
            return;
        }
        if (DUMP_WIFI_SCAN_RESULT) {
            registerReceiver(mWifiScanReceiver,
                    new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        }
        mWiFiScanInterval = Integer.parseInt(
                PreferenceManager.getDefaultSharedPreferences(this).getString(
                getString(R.string.key_locate_wifi_scan_interval),
                getString(R.string.default_locate_wifi_scan_interval)));
        Log.v(TAG, "startWifiScan, interval: " + mWiFiScanInterval);
        mHandler.postDelayed(mWifiScanRunnable, mWiFiScanInterval);
        mIsWifiScanning = true;
    }

    private void stopWifiScan() {
        if (!mIsWifiScanning) {
            return;
        }
        mHandler.removeCallbacks(mWifiScanRunnable);
        if (DUMP_WIFI_SCAN_RESULT) {
            unregisterReceiver(mWifiScanReceiver);
        }
        mIsWifiScanning = false;
    }

    private Runnable mWifiScanRunnable = new Runnable() {
        @Override
        public void run() {
            mWifiManager.startScan();
            mHandler.postDelayed(mWifiScanRunnable, mWiFiScanInterval);
        }
    };

    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            List<ScanResult> results = mWifiManager.getScanResults();
            Collections.sort(results, new Comparator<ScanResult>() {
                @Override
                public int compare(final ScanResult object1, final ScanResult object2) {
                    // sort ascending channel#, descending strength
                    return ((object2.frequency * 1000 - object2.level) -
                            (object1.frequency * 1000 - object1.level));
                }
            });
            for (ScanResult result : results) {
                Log.v(TAG, formatResult(result));
            }
        }
    };

    private String formatResult(ScanResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append(result.frequency + "MHz");
        builder.append(", " + result.SSID);
        return builder.toString();
    }

    private void enableBeaconScan(boolean enable) {
        Log.v(TAG, "enableBeaconScan, " + enable);
        if (enable && mBeaconAPI == null) {
            mBeaconAPI = new BeaconAPI(this, getPackageName());
            mBeaconAPI.setDetectRange(20);
            mBeaconAPI.addBeaconListener(mBeaconListener);
        }
        if (enable) {
            if (mBeaconInterval <= 3000) {
                mBeaconAPI.setBetweenScanPeriod(0);
                mBeaconAPI.setScanPeriod(mBeaconInterval);
            } else {
                mBeaconAPI.setBetweenScanPeriod(mBeaconInterval - 3000);
                mBeaconAPI.setScanPeriod(3000);
            }
        }
        if (mBeaconAPI != null) {
            mBeaconAPI.enableDiscovery(enable);
        }
    }

    private void destroyBeaconScan() {
        if (mBeaconAPI != null) {
            mBeaconAPI.enableDiscovery(false);
            mBeaconAPI.removeBeaconListener(mBeaconListener);
            mBeaconAPI.destroy();
        }
    }

    private BeaconListener mBeaconListener = new BeaconListener() {
        @Override
        public void onRefresh(final List<SimpleBeacon> beacons) {
            if (beacons == null) {
                return;
            }
            // report 10 beacons at most
            List<SimpleBeacon> reportBeacons = beacons.size() > 10 ?
                    beacons.subList(0, 10) : beacons;
            mLocateAgent.reportDevice(
                    getApplicationContext(),
                    mTargetMac,
                    mIndoorMap.getMapProxy().getDegree(),
                    reportBeacons);
        }
    };
}
