package com.ibluetag.indoor;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.*;
import android.widget.Toast;

//配置菜单类
public class DemoPreferenceActivity extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "DemoPrefActivity";

    //map
    private EditTextPreference mMapServerEdit;
    private EditTextPreference mMapSubjectIdEdit;
    private ListPreference mMapLoadModeList;
    private EditTextPreference mMapLoadFloorIdEdit;
    private EditTextPreference mMapLoadInitialLabelEdit;

    //locate
    private EditTextPreference mLocateUpdateIntervalEdit;

    //wifi
    private EditTextPreference mLocateTargetEdit;
    private CheckBoxPreference mLocateWithPhoneCheck;
    private EditTextPreference mLocateWifiScanIntervalEdit;

    //push
    private CheckBoxPreference mPushEnableCheck;
    private EditTextPreference mPushServerEdit;

    //route
    private EditTextPreference mRouteAttachThresholdEdit;
    private EditTextPreference mRouteDeviateThresholdEdit;
    private ListPreference mRouteRuleList;

    //cache
    private CheckBoxPreference mClearCacheCheck;

    private WifiManager mWifiManager;
    private String mPhoneMac;
    private BluetoothAdapter mBluetoothAdapter;
    private String mLastMapServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mPhoneMac = mWifiManager.getConnectionInfo().getMacAddress();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // map settings
        mMapServerEdit = (EditTextPreference) findPreference(
                getString(R.string.key_map_server));
        mLastMapServer = mMapServerEdit.getText();
        mMapServerEdit.setSummary(mLastMapServer);
        mMapServerEdit.setOnPreferenceChangeListener(this);
        mMapSubjectIdEdit = (EditTextPreference) findPreference(
                getString(R.string.key_map_subject_id));
        mMapSubjectIdEdit.setSummary(mMapSubjectIdEdit.getText());
        mMapSubjectIdEdit.setOnPreferenceChangeListener(this);
        mMapLoadModeList = (ListPreference) findPreference(getString(R.string.key_map_load_mode));
        mMapLoadModeList.setSummary(mMapLoadModeList.getEntry());
        mMapLoadModeList.setOnPreferenceChangeListener(this);
        mMapLoadFloorIdEdit = (EditTextPreference) findPreference(
                getString(R.string.key_map_load_initial_floor_id));
        mMapLoadFloorIdEdit.setSummary(mMapLoadFloorIdEdit.getText());
        mMapLoadFloorIdEdit.setOnPreferenceChangeListener(this);
        mMapLoadFloorIdEdit.setEnabled(mMapLoadModeList.getValue().equals(
                getString(R.string.value_map_load_mode_subject)) == false);
        mMapLoadInitialLabelEdit = (EditTextPreference) findPreference(
                getString(R.string.key_map_load_initial_label));
        mMapLoadInitialLabelEdit.setSummary(mMapLoadInitialLabelEdit.getText());
        mMapLoadInitialLabelEdit.setOnPreferenceChangeListener(this);
        mMapLoadInitialLabelEdit.setEnabled(
                mMapLoadModeList.getValue().equals(
                getString(R.string.value_map_load_mode_booth_select)) ||
                mMapLoadModeList.getValue().equals(
                getString(R.string.value_map_load_mode_booth_route)));

        // locate
        mLocateUpdateIntervalEdit = (EditTextPreference) findPreference(
                getString(R.string.key_locate_update_interval));
        mLocateUpdateIntervalEdit.setSummary(mLocateUpdateIntervalEdit.getText());
        mLocateUpdateIntervalEdit.setOnPreferenceChangeListener(this);

        // wifi
        mLocateTargetEdit = (EditTextPreference) findPreference(
                getString(R.string.key_locate_target));
        mLocateTargetEdit.setSummary(mLocateTargetEdit.getText());
        mLocateTargetEdit.setOnPreferenceChangeListener(this);
        mLocateWithPhoneCheck = (CheckBoxPreference) findPreference(
                getString(R.string.key_locate_with_phone));
        mLocateWithPhoneCheck.setOnPreferenceChangeListener(this);
        mLocateWifiScanIntervalEdit = (EditTextPreference) findPreference(
                getString(R.string.key_locate_wifi_scan_interval));
        mLocateWifiScanIntervalEdit.setSummary(mLocateWifiScanIntervalEdit.getText());
        mLocateWifiScanIntervalEdit.setOnPreferenceChangeListener(this);

        // push
        mPushEnableCheck = (CheckBoxPreference) findPreference(
                getString(R.string.key_push_enable));
        mPushEnableCheck.setOnPreferenceChangeListener(this);
        mPushServerEdit = (EditTextPreference) findPreference(
                getString(R.string.key_push_server));
        mPushServerEdit.setSummary(mPushServerEdit.getText());
        mPushServerEdit.setOnPreferenceChangeListener(this);

        // route
        mRouteAttachThresholdEdit = (EditTextPreference) findPreference(
                getString(R.string.key_route_attach_threshold));
        mRouteAttachThresholdEdit.setSummary(mRouteAttachThresholdEdit.getText());
        mRouteAttachThresholdEdit.setOnPreferenceChangeListener(this);
        mRouteDeviateThresholdEdit = (EditTextPreference) findPreference(
                getString(R.string.key_route_deviate_threshold));
        mRouteDeviateThresholdEdit.setSummary(mRouteDeviateThresholdEdit.getText());
        mRouteDeviateThresholdEdit.setOnPreferenceChangeListener(this);
        mRouteRuleList = (ListPreference) findPreference(getString(R.string.key_route_rule));
        mRouteRuleList.setSummary(mRouteRuleList.getEntry());
        mRouteRuleList.setOnPreferenceChangeListener(this);

        // cache
        mClearCacheCheck = (CheckBoxPreference) findPreference(
                getString(R.string.key_clear_cache));
        mClearCacheCheck.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        //map
        if (preference.getKey().equals(getString(R.string.key_map_server))) {
            mMapServerEdit.setSummary((CharSequence) newValue);
            mClearCacheCheck.setChecked(!mLastMapServer.equalsIgnoreCase((String) newValue));
        } else if (preference.getKey().equals(getString(R.string.key_map_subject_id))) {
            mMapSubjectIdEdit.setSummary((CharSequence) newValue);
        } else if (preference.getKey().equals(getString(R.string.key_map_load_mode))) {
            String loadMode = (String) newValue;
            mMapLoadModeList.setSummary(mMapLoadModeList.getEntries()[
                    mMapLoadModeList.findIndexOfValue(loadMode)]);
            mMapLoadFloorIdEdit.setEnabled(loadMode.equals(
                    getString(R.string.value_map_load_mode_subject)) == false);
            mMapLoadInitialLabelEdit.setEnabled(
                    loadMode.equals(getString(R.string.value_map_load_mode_booth_select)) ||
                    loadMode.equals(getString(R.string.value_map_load_mode_booth_route)));
        } else if (preference.getKey().equals(getString(R.string.key_map_load_initial_floor_id))) {
            mMapLoadFloorIdEdit.setSummary((CharSequence) newValue);
        } else if (preference.getKey().equals(getString(R.string.key_map_load_initial_label))) {
            mMapLoadInitialLabelEdit.setSummary((CharSequence) newValue);
        //locate
        } else if (preference.getKey().equals(getString(R.string.key_locate_update_interval))) {
            mLocateUpdateIntervalEdit.setSummary((CharSequence) newValue);
        //wifi
        } else if (preference.getKey().equals(getString(R.string.key_locate_target))) {
            mLocateTargetEdit.setSummary((CharSequence) newValue);
        } else if (preference.getKey().equals(getString(R.string.key_locate_with_phone))) {
            String targetMac = (Boolean) newValue ?
                    mPhoneMac : getString(R.string.default_locate_target);
            mLocateTargetEdit.setText(targetMac.toUpperCase());
            mLocateTargetEdit.setSummary(targetMac.toUpperCase());
        } else if (preference.getKey().equals(getString(R.string.key_locate_wifi_scan_interval))) {
            mLocateWifiScanIntervalEdit.setSummary((CharSequence) newValue);
        //push
        } else if (preference.getKey().equals(getString(R.string.key_push_server))) {
            mPushServerEdit.setSummary((CharSequence) newValue);
        //route
        } else if (preference.getKey().equals(getString(R.string.key_route_attach_threshold))) {
            mRouteAttachThresholdEdit.setSummary((CharSequence) newValue);
        } else if (preference.getKey().equals(getString(R.string.key_route_deviate_threshold))) {
            mRouteDeviateThresholdEdit.setSummary((CharSequence) newValue);
        } else if (preference.getKey().equals(getString(R.string.key_route_rule))) {
            mRouteRuleList.setSummary(mRouteRuleList.getEntries()[
                    mRouteRuleList.findIndexOfValue((String) newValue)]);
        }
        // allow change
        return true;
    }
}
