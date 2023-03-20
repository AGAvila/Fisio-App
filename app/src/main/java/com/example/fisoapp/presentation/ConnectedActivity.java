package com.example.fisoapp.presentation;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.example.fisoapp.R;
import com.example.fisoapp.data.Acquisition;
import com.example.fisoapp.domain.GattAttributes;
import com.example.fisoapp.services.BluetoothLeService;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class ConnectedActivity extends AppCompatActivity {
    private final static String TAG = ConnectedActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    //private final String LIST_NAME = "NAME";
    //private final String LIST_UUID = "UUID";


    private TextView mConnectionState;
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic mCommandCharacteristic;

    private com.github.mikephil.charting.charts.LineChart mLineChart;
    private View mBottomConnect;
    private View mBottomQuit;

    private PreferenceManager mPrefManager;
    private SharedPreferences mPreferences;

    private final static ArrayList<String> prefs =
            new ArrayList<>(Arrays.asList("front","back","coolant","oil","brakeL","brakeP","wasser"));

    private Acquisition mAcquisition;

    TextView rms_value_display; // RMS value display


    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    /*
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {



                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }

                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }


                        return true;
                    }
                    return false;
                }
            };
*/


    @SuppressLint("RestrictedApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected);

        // Creates LineChart use for plotting the data
        mLineChart = new LineChart(this);
        mLineChart = (LineChart) findViewById(R.id.linechart_1);
        mLineChart.setTouchEnabled(true);
        mLineChart.setPinchZoom(false);
        mLineChart.setScaleEnabled(true);

        mBottomConnect = findViewById(R.id.connectButton);
        mBottomQuit = findViewById(R.id.back_button);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        mConnectionState = (TextView) findViewById(R.id.connection_state);

        mPrefManager= new PreferenceManager(this);
        mPreferences = mPrefManager.getSharedPreferences();

        //Set acquisition
        mAcquisition = new Acquisition();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        // Defines the used methods
        renderData();
        quit();
        startAcq();
    }


    public void connect(View view){
        if( !mConnected) {
            mBluetoothLeService.connect(mDeviceAddress);
        }
    }


    public void quit (){
        // Stops Bluetooth connection and goes back to main menu

        Button back_button = (Button) findViewById(R.id.back_button);
        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopAcq(view);
                mBluetoothLeService.disconnect();
                finish();
            }
        });
    }


    public void startAcq(){
        // Starts the data acquisition and the BLE communication

        Button acquisition_button = findViewById(R.id.StartButton);
        acquisition_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mCommandCharacteristic != null) {
                    mCommandCharacteristic.setValue("a");
                    mBluetoothLeService.writeCharacteristic(mCommandCharacteristic);

                    // Acquiring message to user
                    rms_value_display = findViewById(R.id.rms_value_display);
                    rms_value_display.setText(R.string.acquiring_message);
                }
            }
        });
    }


    public void stopAcq(View view){
        // Stops the acquisition of data and the BLE communication

        if (mCommandCharacteristic != null){
            mCommandCharacteristic.setValue("q");
            mBluetoothLeService.writeCharacteristic(mCommandCharacteristic);
        }
    }


    private void saveData() {
        // Save the plotted data in a csv file

        short[] short_data = mAcquisition.getConvertedData();
        String file_name = "DataPointsStorage";

        try {
            mAcquisition.saveAcquisition(short_data, file_name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.connected_menu, menu);
        if (!mConnected) {
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_connect).setVisible(true);

        } else {
            menu.findItem(R.id.menu_disconnect).setVisible(true);
            menu.findItem(R.id.menu_connect).setVisible(false);
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                break;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                break;
        }
        return true;
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }


    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }


    private void discoverGattServices(List<BluetoothGattService> gattServices) {
        // Inicializar las notificaciones y extraer characteristica de commandos

        if (gattServices == null) return;
        //Inicio notificación status
        BluetoothGattCharacteristic TxChar = gattServices.get(2).getCharacteristic(
                UUID.fromString(GattAttributes.TX_CHAR)
        );

        int charaProp = TxChar.getProperties();
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mBluetoothLeService.setCharacteristicNotification(TxChar, true);
        }

        mCommandCharacteristic = gattServices.get(2).getCharacteristic(
                UUID.fromString(GattAttributes.RX_CHAR)
        );

/*
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();


        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            String CommandUuid = GattAttributes.COMMANDS_CHARACTERISTIC;
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, GattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                //save CommandCharacteristic
                if(uuid.equals(CommandUuid)){
                    mCommandCharacteristic = gattCharacteristic.getService().getCharacteristic(UUID.fromString(uuid));
                }

                currentCharaData.put(
                        LIST_NAME, GattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);

        }

         */


    }


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }


        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };


    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device. This can be a result of read or
    // notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                ImageView view = findViewById(R.id.conn_image);
                view.setVisibility(View.VISIBLE);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                ImageView view = findViewById(R.id.conn_image);
                view.setVisibility(View.INVISIBLE);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                discoverGattServices(mBluetoothLeService.getSupportedGattServices());

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                // Go here if there is info ready to be stored

                byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                mAcquisition.addPacket(data);

                // Plot and save the data if the buffer is full
                if (mAcquisition.isReadyToPlot()){
                    mAcquisition.updateReadyToPlot(false);
                    mAcquisition.adaptSamples(mAcquisition.getData());
                    updateGraphics();
                    saveData();
                }
            }
        }
    };


/*
    private final View.OnClickListener ImageClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int image = v.getId();
            int index = 0xFF;
        }
    };
*/


    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


    private void updateGraphics(){
        // Displays the RMS value and plot the signal

        double RMS = mAcquisition.calculateRMS(mAcquisition.getConvertedData());

        // Adapt RMS to mV
        double ADC_precision = 4.5013e-05;
        double ADC_bottom_limit = 0.1;
        double mRMS = (RMS * ADC_precision + ADC_bottom_limit) * 1000;

        // Displays mRMS value in screen
        String mRMS_display = "RMS: " + mRMS + " mV";
        rms_value_display = findViewById(R.id.rms_value_display);
        rms_value_display.setText(mRMS_display);

        if (mRMS>2){
            rms_value_display.setTextColor(Color.RED);
        }
        else {
            rms_value_display.setTextColor(Color.GREEN);
        }

        // Update mlineChart with the data from mAcquisition
        renderData();
    }


    public void renderData(){
        // Configures the axis for the LineChart before plotting and then calls the function to plot

        // Horizontal axis configuration
        XAxis xAxis = mLineChart.getXAxis();
        xAxis.enableGridDashedLine(10f, 10f, 0f);
        xAxis.setAxisMaximum(10f); // Represent 10 seconds of the signal
        xAxis.setAxisMinimum(0f);

        // Vertical axis configuration
        YAxis leftAxis = mLineChart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        leftAxis.setDrawZeroLine(false);
        leftAxis.setAxisMinimum(0f);

        mLineChart.getAxisRight().setEnabled(false);
        setData();
    }


    private void setData() {
        // Plot the received 2 bytes data

        short[] short_data = mAcquisition.getConvertedData();
        int data_array_length = short_data.length;

        // Conversion: Byte[] -> int[]
        int[] int_data = new int[data_array_length];
        for (int i = 0; i < data_array_length; i++){
            int_data[i] = short_data[i] & 0xFFFF;
        }

        // Conversion: int[] -> float[]
        float[] float_data = new float[data_array_length];
        for (int i = 0; i < data_array_length; i++){
            float_data[i] = (float) int_data[i];
        }

        // Conversion from samples to milivolts
        double ADC_precision = 4.5013e-05;
        float ADC_bottom_limit = (float) 0.1;
        float[] mV_data = new float[data_array_length];
        for (int i = 0; i < data_array_length; i++){
            mV_data[i] = (float) (float_data[i] * ADC_precision + 0.1) * 1000;
        }

        // Add values to entry list
        ArrayList<Entry> data_points = new ArrayList<>();
        //float f;
        float j = 0;
        for (int i = 0; i < data_array_length; i++){
            //f = mV_data[i];
            j += 0.001;
            data_points.add(new Entry(j, mV_data[i]));
        }

        LineDataSet plot1;
        plot1 = new LineDataSet(data_points, "ECG");
        LineData data = new LineData(plot1);
        plot1.setDrawCircles(false);
        plot1.setLineWidth(0f);
        plot1.setColor(Color.argb(255,25,190,190));

        mLineChart.setData(data);
    }


} //End class