package com.example.fisoapp.presentation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import com.example.fisoapp.R;
import com.example.fisoapp.domain.GattAttributes;

import java.util.ArrayList;

public class ScanActivity extends AppCompatActivity {


    private LocationManager mLocationManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_SCANN_BT = 2;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private ListView mScannerList;
    private boolean mScanning;
    private Handler mHandler;

    //   private PreferenceManager mPrefManager;
    //   private SharedPreferences mPreferences;


    private ScanFilter mScanFilter;
    private ArrayList<ScanFilter> filters = new ArrayList<ScanFilter>();
    private ScanSettings mScanSettings;
    private ParcelUuid CheckControlUUID;

    private static final int PERMISSION_REQUEST_LOCATION = 1;
    private static final int PERMISSION_REQUEST_SCANN = 2;
    private static final int PERMISSION_REQUEST_CONNECT = 3;
    private static final int PERMISSION_REQUEST_BLUETOOTH = 4;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    @SuppressLint("RestrictedApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);
        mHandler = new Handler();

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mScannerList = findViewById(R.id.scanned_list);


        CheckControlUUID = ParcelUuid.fromString(GattAttributes.CHECKCONTROL_SERVICE);
        mScanFilter = new ScanFilter.Builder()
                .setServiceUuid(CheckControlUUID)
                //.setDeviceName("Kadett GSi 8v")
                //setDeviceAddress("00:A0:50:D1:36:47").
                .build();
        filters.add(mScanFilter);

        mScanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.scan_menu, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);

        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device. If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

        }

        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        try {
            network_enabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        if (!gps_enabled && !network_enabled) {
            // notify user
            new AlertDialog.Builder(this)
                    .setMessage(R.string.gps_network_not_enabled)
                    .setPositiveButton(R.string.open_location_settings, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mScannerList.setAdapter(mLeDeviceListAdapter);
        mScannerList.setOnItemClickListener(ListClickListner);

        //Check permissions needed along the activity

        if( Build.VERSION.SDK_INT >=Build.VERSION_CODES.S ){//Android 12 or higher //ToDo: Check correct working of this section


         if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermission(Manifest.permission.BLUETOOTH_SCAN, "Necesito Bluetooth", PERMISSION_REQUEST_SCANN, this);

        }
        else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {

        requestPermission(Manifest.permission.BLUETOOTH_CONNECT, "necesito connect", PERMISSION_REQUEST_CONNECT,
                    this);
        }}

        else if(ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) // Android 11 or lower
                != PackageManager.PERMISSION_GRANTED) {

            requestPermission(Manifest.permission.BLUETOOTH, "necesito connect", PERMISSION_REQUEST_BLUETOOTH,
                    this);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermission(Manifest.permission.ACCESS_FINE_LOCATION,
                    "Sin el permiso localización no es posible escanear dispositivos" +
                            " Bluetooth.", PERMISSION_REQUEST_LOCATION, this);

        }
        else {
            scanLeDevice(true);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    private final ListView.OnItemClickListener ListClickListner =
            new AdapterView.OnItemClickListener() {
                @SuppressLint("MissingPermission")
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
                    if (device == null) return;
                                      final Intent intent = new Intent(ScanActivity.this, ConnectedActivity.class);
                                      intent.putExtra(ConnectedActivity.EXTRAS_DEVICE_NAME, device.getName());
                                      intent.putExtra(ConnectedActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                    if (mScanning) {
                        mBluetoothLeScanner.stopScan(mLeScanCallback);
                        mScanning = false;
                    }
                    startActivity(intent);


                }
            };


    @SuppressLint("MissingPermission")
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
//        final Intent intent = new Intent(this, ConnectedActivity.class);
//        intent.putExtra(ConnectedActivity.EXTRAS_DEVICE_NAME, device.getName());
//        intent.putExtra(ConnectedActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            mBluetoothLeScanner.stopScan(mLeScanCallback);
            mScanning = false;
        }
//        startActivity(intent);
    }


    @Override
    protected void onPause() {
        super.onPause();
        if(mScanning == true){
        scanLeDevice(false);
        }
        mLeDeviceListAdapter.clear();
    }

    @SuppressLint("MissingPermission")
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @SuppressLint("MissingPermission")
                @Override
                public void run() {
                    mScanning = false;

                    mBluetoothLeScanner.stopScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothLeScanner.startScan(null, mScanSettings, mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothLeScanner.stopScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    private static void requestPermission(final String permiso, String
            justificacion, final int requestCode, final Activity actividad) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(actividad,
                permiso)) {
            new AlertDialog.Builder(actividad)
                    .setTitle("Solicitud de permiso")
                    .setMessage(justificacion)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            ActivityCompat.requestPermissions(actividad,
                                    new String[]{permiso}, requestCode);
                        }
                    }).show();
        } else {
            ActivityCompat.requestPermissions(actividad,
                    new String[]{permiso}, requestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case PERMISSION_REQUEST_SCANN:
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this,"No se puede escanear sin el permiso" +
                            " de escaneo",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case PERMISSION_REQUEST_LOCATION:
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this,"No se puede escanear sin el permiso " +
                            " de localización",Toast.LENGTH_SHORT).show();

                    finish();
                }
                break;
            case PERMISSION_REQUEST_CONNECT:
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this,"No se puede escanear sin el permiso " +
                            " de conexión",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

            case PERMISSION_REQUEST_BLUETOOTH:
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this,"No se puede escanear sin acceso a" +
                            " Bluetooth",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                break;


        }
    }


    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = ScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.list_item, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            @SuppressLint("MissingPermission")
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }

    }
    // Device scan callback.
    private ScanCallback mLeScanCallback =
            new ScanCallback() {

                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType,result);
                    mLeDeviceListAdapter.addDevice(result.getDevice());
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            };


    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}
