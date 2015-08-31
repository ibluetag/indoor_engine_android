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

public class DemoPreferenceActivity extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "DemoPrefActivity";

    private EditTextPreference mMapServerEdit;
    private EditTextPreference mMapSubjectIdEdit;
    private EditTextPreference mLocateTargetEdit;
    private CheckBoxPreference mLocateWithPhoneCheck;
    private EditTextPreference mLocateWifiScanIntervalEdit;
    private CheckBoxPreference mLocateBeaconDiscoveryCheck;
    private EditTextPreference mLocateBeaconScanIntervalEdit;
    private EditTextPreference mLocateBeaconServerEdit;
    private EditTextPreference mLocateBeaconPortEdit;
    private CheckBoxPreference mPushEnableCheck;
    private EditTextPreference mPushServerEdit;
    private EditTextPreference mRouteAttachThresholdEdit;
    private EditTextPreference mRouteDeviateThresholdEdit;
    private ListPreference mRouteRuleList;
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

        // beacon
        mLocateBeaconDiscoveryCheck = (CheckBoxPreference) findPreference(
                getString(R.string.key_locate_beacon_discovery));
        mLocateBeaconDiscoveryCheck.setOnPreferenceChangeListener(this);
        mLocateBeaconScanIntervalEdit = (EditTextPreference) findPreference(
                getString(R.string.key_locate_beacon_scan_interval));
        mLocateBeaconScanIntervalEdit.setSummary(mLocateBeaconScanIntervalEdit.getText());
        mLocateBeaconScanIntervalEdit.setOnPreferenceChangeListener(this);
        mLocateBeaconServerEdit = (EditTextPreference) findPreference(
                getString(R.string.key_locate_beacon_server));
        mLocateBeaconServerEdit.setSummary(mLocateBeaconServerEdit.getText());
        mLocateBeaconServerEdit.setOnPreferenceChangeListener(this);
        mLocateBeaconPortEdit = (EditTextPreference) findPreference(
                getString(R.string.key_locate_beacon_port));
        mLocateBeaconPortEdit.setSummary(mLocateBeaconPortEdit.getText());
        mLocateBeaconPortEdit.setOnPreferenceChangeListener(this);

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
        if (preference.getKey().equals(getString(R.string.key_map_server))) {
            mMapServerEdit.setSummary((CharSequence) newValue);
            mClearCacheCheck.setChecked(!mLastMapServer.equalsIgnoreCase((String) newValue));
        } else if (preference.getKey().equals(getString(R.string.key_map_subject_id))) {
            mMapSubjectIdEdit.setSummary((CharSequence) newValue);
        } else if (preference.getKey().equals(getString(R.string.key_locate_target))) {
            mLocateTargetEdit.setSummary((CharSequence) newValue);
        } else if (preference.getKey().equals(getString(R.string.key_locate_with_phone))) {
            String targetMac = (Boolean) newValue ?
                    mPhoneMac : getString(R.string.default_locate_target);
            mLocateTargetEdit.setText(targetMac.toUpperCase());
            mLocateTargetEdit.setSummary(targetMac.toUpperCase());
        } else if (preference.getKey().equals(getString(R.string.key_locate_wifi_scan_interval))) {
            mLocateWifiScanIntervalEdit.setSummary((CharSequence) newValue);
        } else if (preference.getKey().equals(getString(R.string.key_locate_beacon_discovery))) {
            if ((Boolean) newValue) {
                if (mBluetoothAdapter == null) {
                    Toast.makeText(getApplicationContext(), R.string.toast_bluetooth_invalid,
                            Toast.LENGTH_SHORT).show();
                    return false;
                } else if (!mBluetoothAdapter.isEnabled()) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.beacon_enable_bluetooth_title)
                            .setMessage(R.string.beacon_enable_bluetooth_message)
                            .setPositiveButton(R.string.btn_confirm,
                                    new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(
                                            BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                    startActivity(intent);
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton(R.string.btn_cancel,
                                    new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).show();
                    return false;
                }
            }
        } else if (preference.getKey().equals(getString(R.string.key_locate_beacon_scan_interval))) {
            mLocateBeaconScanIntervalEdit.setSummary((CharSequence) newValue);
        } else if (preference.getKey().equals(getString(R.string.key_locate_beacon_server))) {
            mLocateBeaconServerEdit.setSummary((CharSequence) newValue);
        } else if (preference.getKey().equals(getString(R.string.key_locate_beacon_port))) {
            mLocateBeaconPortEdit.setSummary((CharSequence) newValue);
        } else if (preference.getKey().equals(getString(R.string.key_push_server))) {
            mPushServerEdit.setSummary((CharSequence) newValue);
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
