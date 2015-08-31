package com.ibluetag.indoor;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

public class DemoSettingsActivity extends Activity {
    public static final String SHARED_PREF_NAME = "indoor_demo_settings";
    public static final String KEY_MAP_SERVER = "indoor_map_server";
    public static final String KEY_MAP_SUBJECT_ID = "indoor_map_subject_id";
    public static final String KEY_LOCATE_SERVER = "indoor_locate_server";
    public static final String KEY_LOCATE_MAC = "indoor_locate_mac";
    public static final String DEFAULT_MAP_SERVER = "http://a.imapview.com/";
    // 地图主体唯一标识符，向触景地图服务提供商申请获取
    public static final int DEFAULT_MAP_SUBJECT_ID = 1;
    public static final String DEFAULT_LOCATE_SERVER = "http://www.uradiosys.com:8063";
    public static final String DEFAULT_LOCATE_MAC = "B0:8E:1A:50:3D:61";

    private EditText mMapServerEdit;
    private EditText mMapSubjectEdit;
    private EditText mLocateServerEdit;
    private EditText mLocateMacEdit;
    private CheckBox mPhoneMacCheckBox;
    private SharedPreferences mSharedPref;
    private WifiManager mWifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle(R.string.settings_title);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mSharedPref = getSharedPreferences(SHARED_PREF_NAME, 0);
        mMapServerEdit = (EditText) findViewById(R.id.map_server_edit);
        mMapSubjectEdit = (EditText) findViewById(R.id.map_subject_edit);
        mLocateServerEdit = (EditText) findViewById(R.id.locate_server_edit);
        mLocateMacEdit = (EditText) findViewById(R.id.locate_mac_edit);
        mPhoneMacCheckBox = (CheckBox) findViewById(R.id.locate_use_phone_mac);
        findViewById(R.id.confirm_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long mapSubjectId = -1;
                String mapServer = mMapServerEdit.getText().toString();
                String locateServer = mLocateServerEdit.getText().toString();
                String locateMac = mLocateMacEdit.getText().toString();
                try {
                    mapSubjectId = Long.valueOf(mMapSubjectEdit.getText().toString());
                } catch (NumberFormatException e) {
                    Toast.makeText(getApplicationContext(), R.string.toast_settings_invalid,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mapServer.isEmpty() || mapSubjectId <= 0 ||
                        locateServer.isEmpty() || locateMac.isEmpty()) {
                    Toast.makeText(getApplicationContext(), R.string.toast_settings_invalid,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                SharedPreferences.Editor editor = mSharedPref.edit();
                editor.putString(KEY_MAP_SERVER, mapServer);
                editor.putLong(KEY_MAP_SUBJECT_ID, mapSubjectId);
                editor.putString(KEY_LOCATE_SERVER, locateServer);
                editor.putString(KEY_LOCATE_MAC, locateMac);
                editor.commit();

                setResult(RESULT_OK);
                finish();
            }
        });

        findViewById(R.id.cancel_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mPhoneMacCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mWifiManager == null) {
                    return;
                }
                if (isChecked) {
                    String phoneMac = mWifiManager.getConnectionInfo().getMacAddress();
                    if (phoneMac != null && !phoneMac.isEmpty()) {
                        mLocateMacEdit.setText(phoneMac);
                    }
                } else {
                    mLocateMacEdit.setText(DEFAULT_LOCATE_MAC);
                }
            }
        });

        String savedMapServer = mSharedPref.getString(KEY_MAP_SERVER, DEFAULT_MAP_SERVER);
        long savedId = mSharedPref.getLong(KEY_MAP_SUBJECT_ID, DEFAULT_MAP_SUBJECT_ID);
        String savedLocateServer = mSharedPref.getString(KEY_LOCATE_SERVER, DEFAULT_LOCATE_SERVER);
        String savedLocateMac = mSharedPref.getString(KEY_LOCATE_MAC, DEFAULT_LOCATE_MAC);
        if (savedMapServer != null) {
            mMapServerEdit.setText(savedMapServer);
        }
        if (savedId > 0) {
            mMapSubjectEdit.setText(String.valueOf(savedId));
        }
        if (savedLocateServer != null) {
            mLocateServerEdit.setText(savedLocateServer);
        }
        if (savedLocateMac != null) {
            mLocateMacEdit.setText(savedLocateMac);
        }
    }
}
