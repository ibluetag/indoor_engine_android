package com.ibluetag.indoor;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Bundle;
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

public class DemoIndoorActivity extends Activity {
    private static final String TAG = "DemoIndoorActivity";
    public static final int REQUEST_CODE_SETTINGS = 0x01;

    private TextView mTitle;
    private ImageButton mSearchBtn;
    private View mSearchInputLayout;
    private EditText mSearchInputEdit;
    private ImageButton mSearchInputClearBtn;
    private Button mSearchCancelBtn;

    private IndoorMapView mIndoorMap;
    private Building mCurrentBuilding;
    private long mCurrentFloorId = 1;
    private BitmapOverlay mBitmapOverlay1;
    private BitmapOverlay mBitmapOverlay2;
    private DemoLocateAgent mLocateAgent = new DemoLocateAgent();
    private SharedPreferences mSharedPref;
    private String mMapServerUrl;
    private long mMapSubjectId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_indoor_map);
        setupTitleBar();
        setupIndoorMap();
        setupOverlay();
        reloadMap();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLocateAgent.isStarted()) {
            mLocateAgent.stop();
        }
        mBitmapOverlay1.destroy();
        mBitmapOverlay2.destroy();
        mIndoorMap.getMapProxy().destroy();
    }

    private void reloadMap() {
        mSharedPref = getSharedPreferences(DemoSettingsActivity.SHARED_PREF_NAME, 0);
        mMapServerUrl = mSharedPref.getString(DemoSettingsActivity.KEY_MAP_SERVER,
                DemoSettingsActivity.DEFAULT_MAP_SERVER);
        mMapSubjectId = mSharedPref.getLong(DemoSettingsActivity.KEY_MAP_SUBJECT_ID,
                DemoSettingsActivity.DEFAULT_MAP_SUBJECT_ID);
        mLocateAgent.setServer(mSharedPref.getString(DemoSettingsActivity.KEY_LOCATE_SERVER,
                DemoSettingsActivity.DEFAULT_LOCATE_SERVER));
        mLocateAgent.setTarget(mSharedPref.getString(DemoSettingsActivity.KEY_LOCATE_MAC,
                DemoSettingsActivity.DEFAULT_LOCATE_MAC));
        Log.v(TAG, "reloadMap, map server: " + mMapServerUrl + ", id: " + mMapSubjectId);
        // 设置地图服务器
        mIndoorMap.getMapProxy().initServer(mMapServerUrl);
        // 通过地图主体（可能是商场/机构/场所等）唯一标识符加载地图
        mIndoorMap.getMapProxy().load(mMapSubjectId);
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
                mTitle.setVisibility(View.GONE);
                mSearchBtn.setVisibility(View.GONE);
                mSearchInputLayout.setVisibility(View.VISIBLE);
                mSearchCancelBtn.setVisibility(View.VISIBLE);
            }
        });

        mSearchCancelBtn = (Button) findViewById(R.id.venue_map_search_cancel_btn);
        mSearchCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
        mIndoorMap = (IndoorMapView) findViewById(R.id.indoor_map);
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
            }
        });
        // 设置定位监听
        mIndoorMap.getMapProxy().setLocateListener(new MapProxy.LocateListener() {
            @Override
            public void onLocateNotInBuilding() {
                Toast.makeText(getApplicationContext(), R.string.toast_locate_not_in_building,
                        Toast.LENGTH_LONG).show();
                return;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(getApplicationContext(), DemoSettingsActivity.class);
                startActivityForResult(intent, REQUEST_CODE_SETTINGS);
                return true;
            case R.id.action_start_runtime_locate:
                mIndoorMap.getMapProxy().enableLocating(true);
                return true;
            case R.id.action_stop_runtime_locate:
                mIndoorMap.getMapProxy().enableLocating(false);
                return true;
            case R.id.action_add_overlay:
                mBitmapOverlay1.attach(mCurrentBuilding.getId(), mCurrentFloorId);
                mBitmapOverlay2.attach(mCurrentBuilding.getId(), mCurrentFloorId);
                return true;
            case R.id.action_remove_overlay:
                mBitmapOverlay1.detach();
                mBitmapOverlay2.detach();
                return true;
            case R.id.action_stair_first:
                mIndoorMap.getMapProxy().setRouteRule(MapProxy.ROUTE_RULE_STAIR_FIRST);
                return true;
            case R.id.action_auto_walk_first:
                mIndoorMap.getMapProxy().setRouteRule(MapProxy.ROUTE_RULE_AUTO_WALK_FIRST);
                return true;
            case R.id.action_elevator_first:
                mIndoorMap.getMapProxy().setRouteRule(MapProxy.ROUTE_RULE_ELEVATOR_FIRST);
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SETTINGS:
                if (resultCode == RESULT_OK) {
                    reloadMap();
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
