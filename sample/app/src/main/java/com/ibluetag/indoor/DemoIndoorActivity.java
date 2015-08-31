package com.ibluetag.indoor;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.ibluetag.indoor.sdk.model.BitmapOverlay;
import com.ibluetag.indoor.sdk.model.Building;
import com.ibluetag.indoor.sdk.IndoorMapView;
import com.ibluetag.indoor.sdk.MapProxy;

public class DemoIndoorActivity extends Activity {
    private static final String TAG = "DemoIndoorActivity";
    // 地图主题唯一标识符，向触景地图服务提供商申请获取
    private static final int DEMO_SUBJECT_ID = 1;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_indoor_map);
        setupTitleBar();
        setupIndoorMap();
        setupOverlay();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBitmapOverlay1.destroy();
        mBitmapOverlay2.destroy();
        mIndoorMap.getMapProxy().destroy();
    }

    private void setupTitleBar() {
        mTitle = (TextView) findViewById(R.id.title);
        mTitle.setText(R.string.event_name);
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
        // 通过地图主体（可能是商场/机构/场所等）唯一标识符加载地图
        mIndoorMap.getMapProxy().load(DEMO_SUBJECT_ID);
        // 设置定位代理
        mIndoorMap.getMapProxy().setLocateAgent(new DemoLocateAgent());
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
}
