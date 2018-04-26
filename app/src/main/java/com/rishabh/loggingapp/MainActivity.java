package com.rishabh.loggingapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telecom.TelecomManager;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener {

    private static final int GPS_PERMISSION_CODE = 123;
    private static final String TAG = "DebugMain";
    private SensorManager mSensorManager;
    private LocationManager mLocationManager;
    private TelephonyManager mTelephonyManager;
    private WifiManager mWifiManager;
    private MediaRecorder mMediaRecorder;
    private Sensor accelerometer, gyroscope;
    private TextView accelerometerTextView, gyroscopeTextView, gpsTextView, wifiTextView, micTextView, networkTextView;
    private Button startButton, stopButton, exportButton;
    private DBHelper mDBHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDBHelper = new DBHelper(this);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mTelephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Initialize views
        accelerometerTextView = (TextView) findViewById(R.id.accelerometer);
        gyroscopeTextView = (TextView) findViewById(R.id.gyroscope);
        gpsTextView = (TextView) findViewById(R.id.gps);
        wifiTextView = (TextView) findViewById(R.id.wifi);
        micTextView = (TextView) findViewById(R.id.mic);
        networkTextView = (TextView) findViewById(R.id.network);
        exportButton = (Button) findViewById(R.id.export);

        // Request permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.RECORD_AUDIO}, GPS_PERMISSION_CODE);
        } else {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
            getNetworkData();
            startRecording();
            getMicData();
        }
        mWifiManager.startScan();

        // Set Wifi Broadcast receiver
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                List<ScanResult> results = mWifiManager.getScanResults();
                Log.d(TAG, "Size: " + results.size());
                StringBuilder message = new StringBuilder("WiFi list:\n");
                for (int i=0;i<results.size();i++) {
                    message.append(i + 1).append(". ").append(results.get(i).SSID).append("\n");
                    mDBHelper.insertWiFiData(results.get(i).SSID);
                }
                wifiTextView.setText(message.toString());
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    private void getMicData() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (mMediaRecorder!=null) {
                    Double decibel = Math.abs(20*Math.log10(mMediaRecorder.getMaxAmplitude()/30));
                    String message = "Mic data: " + decibel + "db";
                    mDBHelper.insertMicData(decibel);
                }
            }
        }, 0, 1000);

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exportToCSV();
            }
        });
    }

    private void startRecording() {
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setOutputFile("/dev/null");
        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaRecorder.start();
    }

    private void stopRecording() {
        try {
            mMediaRecorder.stop();
            mMediaRecorder.release();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        mMediaRecorder = null;
    }

    private void getNetworkData() {
        List<CellInfo> list = null;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                list = mTelephonyManager.getAllCellInfo();
            }
            Log.d(TAG, "Size: " + list.size());
            StringBuilder message = new StringBuilder("Cell Tower data: \n");
            for (int i=0;i<list.size();i++) {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        CellInfoLte cellInfo = (CellInfoLte) list.get(i);
                        message.append(i+1).append(". ").append(cellInfo.getCellIdentity().getCi()).append("\n");
                        mDBHelper.insertNetworkData(cellInfo.getCellIdentity().getCi());
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            networkTextView.setText(message);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecording();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch (sensorEvent.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                String accelerometerText = "Accelerometer - X: " + sensorEvent.values[0] + ", Y: " + sensorEvent.values[1] + ", Z: " + sensorEvent.values[2] + "\n";
                mDBHelper.insertAccelerometerData(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
                accelerometerTextView.setText(accelerometerText);
                break;
            case Sensor.TYPE_GYROSCOPE:
                String gyroscopeText = "Gyroscope - X: " + sensorEvent.values[0] + ", Y: " + sensorEvent.values[1] + ", Z: " + sensorEvent.values[2] + "\n";
                mDBHelper.insertGyroscopeData(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
                gyroscopeTextView.setText(gyroscopeText);
                break;
            default:
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == GPS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "permission granted");
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                    mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
                    getNetworkData();
                    startRecording();
                    getMicData();
                }
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        String locationText = "GPS - Latitude: " + location.getLatitude() + ", Longitude" + location.getLongitude() + "\n";
        mDBHelper.insertGPSData(location.getLatitude(), location.getLongitude());
        gpsTextView.setText(locationText);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {
        Toast.makeText(this, "GPS enabled!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String s) {
        Toast.makeText(this, "GPS disabled!", Toast.LENGTH_SHORT).show();
    }

    public void exportToCSV() {
        File dirName = new File(Environment.getExternalStorageDirectory(), "LoggingApp");
        if (!dirName.exists()) {
            dirName.mkdirs();
        }

        File fileName1 = new File(dirName, "acceleration.csv");
        try {
            fileName1.createNewFile();
            CSVWriter writer = new CSVWriter(new FileWriter(fileName1));
            SQLiteDatabase sqLiteDatabase = mDBHelper.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM Accelerometer", null);
            writer.writeNext(cursor.getColumnNames());
            while (cursor.moveToNext()) {
                String strings[] = {cursor.getString(0), cursor.getString(1), cursor.getString(2)};
                writer.writeNext(strings);
            }
            writer.close();
            cursor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        File fileName2 = new File(dirName, "gyroscope.csv");
        try {
            fileName2.createNewFile();
            CSVWriter writer = new CSVWriter(new FileWriter(fileName2));
            SQLiteDatabase sqLiteDatabase = mDBHelper.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM Gyroscope", null);
            writer.writeNext(cursor.getColumnNames());
            while (cursor.moveToNext()) {
                String strings[] = {cursor.getString(0), cursor.getString(1), cursor.getString(2)};
                writer.writeNext(strings);
            }
            writer.close();
            cursor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        File fileName3 = new File(dirName, "gps.csv");
        try {
            fileName3.createNewFile();
            CSVWriter writer = new CSVWriter(new FileWriter(fileName3));
            SQLiteDatabase sqLiteDatabase = mDBHelper.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM GPS", null);
            writer.writeNext(cursor.getColumnNames());
            while (cursor.moveToNext()) {
                String strings[] = {cursor.getString(0), cursor.getString(1)};
                writer.writeNext(strings);
            }
            writer.close();
            cursor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        File fileName4 = new File(dirName, "wifi.csv");
        try {
            fileName4.createNewFile();
            CSVWriter writer = new CSVWriter(new FileWriter(fileName4));
            SQLiteDatabase sqLiteDatabase = mDBHelper.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM WiFi", null);
            writer.writeNext(cursor.getColumnNames());
            while (cursor.moveToNext()) {
                String strings[] = {cursor.getString(0)};
                writer.writeNext(strings);
            }
            writer.close();
            cursor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        File fileName5 = new File(dirName, "mic.csv");
        try {
            fileName5.createNewFile();
            CSVWriter writer = new CSVWriter(new FileWriter(fileName5));
            SQLiteDatabase sqLiteDatabase = mDBHelper.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM Mic", null);
            writer.writeNext(cursor.getColumnNames());
            while (cursor.moveToNext()) {
                String strings[] = {cursor.getString(0)};
                writer.writeNext(strings);
            }
            writer.close();
            cursor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        File fileName6 = new File(dirName, "network.csv");
        try {
            fileName6.createNewFile();
            CSVWriter writer = new CSVWriter(new FileWriter(fileName6));
            SQLiteDatabase sqLiteDatabase = mDBHelper.getReadableDatabase();
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM Network", null);
            writer.writeNext(cursor.getColumnNames());
            while (cursor.moveToNext()) {
                String strings[] = {cursor.getString(0)};
                writer.writeNext(strings);
            }
            writer.close();
            cursor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
