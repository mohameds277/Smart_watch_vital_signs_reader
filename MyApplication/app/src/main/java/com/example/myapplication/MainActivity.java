package com.example.myapplication;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;




import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
import java.util.UUID;


public class MainActivity extends Activity implements SensorEventListener , GoogleApiClient.ConnectionCallbacks , GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "MainActivity";


    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    int steps = 0;

    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private SensorManager sensorManager;
    private Sensor heartRateSensor;
    private Sensor stepCounterSensor;


//    private Sensor spo2Sensor;
//    private Sensor sleepSensor;
//    private Sensor caloriesSensor;

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;


    TextView heartRateTextView;

    TextView spo2TextView;

    TextView sleepTextView;

    TextView caloriesTextView;

    private Sensor accelerometerSensor;

    private static final float MOVEMENT_THRESHOLD = 13.0f;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Intent serviceIntent = new Intent(this, BackgroundService.class);
        startService(serviceIntent);






        heartRateTextView = findViewById(R.id.heartRateTextView);
        spo2TextView = findViewById(R.id.spo2TextView);
        sleepTextView = findViewById(R.id.sleepTextView);
        caloriesTextView = findViewById(R.id.caloriesTextView);


        Random random = new Random();
        int spo2_level = random.nextInt(8) + 90;
        spo2TextView.setText("SPO2: " + spo2_level +"%");





        // Check and request necessary permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BODY_SENSORS}, PERMISSION_REQUEST_CODE);
        }

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Get references to the specific sensors
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);



    }

    @Override
    protected void onResume() {
        super.onResume();

        // Register sensor listeners
        sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);



    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister sensor listeners to avoid unnecessary sensor data
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_HEART_RATE) {
            // Handle heart rate data
            float heartRate = event.values[0];
            Log.d(TAG, "Heart rate updated: " + heartRate);
            heartRateTextView.setText("Heart Rate: " + heartRate);
            sendHeartRateToBluetoothDevice(heartRate);
        }

        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Get acceleration values along the x, y, and z axes
            float xAxisAcceleration = event.values[0];
            float yAxisAcceleration = event.values[1];
            float zAxisAcceleration = event.values[2];

            // Calculate the magnitude of acceleration
            float accelerationMagnitude = (float) Math.sqrt(
                    Math.pow(xAxisAcceleration, 2) +
                            Math.pow(yAxisAcceleration, 2) +
                            Math.pow(zAxisAcceleration, 2)
            );

            // Check if the magnitude of acceleration exceeds the threshold
            if (accelerationMagnitude > MOVEMENT_THRESHOLD) {
                // Device has moved
                Log.d(TAG, "Device moved" );
                steps++ ;
                caloriesTextView.setText("Steps  " + steps);


            } else {
                // Device is stationary
                Log.d(TAG, "Device static" );
            }
        }
        }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }
    @SuppressLint("MissingPermission")
    private void connectToBluetoothDevice(String deviceAddress) {
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);

        // Establish a Bluetooth connection
        try {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            Log.d(TAG, "Connected to Bluetooth device");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to connect to Bluetooth device");
        }
    }

    private void disconnectFromBluetoothDevice() {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
            Log.d(TAG, "Disconnected from Bluetooth device");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to disconnect from Bluetooth device");
        }
    }

    private void sendHeartRateToBluetoothDevice(float heartRate) {
        if (outputStream != null) {
            String heartRateData = String.valueOf(heartRate);

            try {
                outputStream.write(heartRateData.getBytes());
                outputStream.flush();
                Log.d(TAG, "Heart rate data sent: " + heartRateData);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Failed to send heart rate data");
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
