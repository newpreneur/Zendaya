package club.cycl.zendaya;


import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.navigation.Navigation;
import androidx.navigation.NavigatorProvider;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.navigation.base.options.NavigationOptions;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.MapboxNavigationProvider;
import com.mapbox.navigation.core.trip.session.LocationMatcherResult;
import com.mapbox.navigation.core.trip.session.LocationObserver;
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;

public class CyClCoreService extends Service implements SensorEventListener, PermissionsListener {
    private static final String TAG = CyClCoreService.class.getName();
    private SensorManager mSensorManager = null;
    private HashMap<String, Sensor> mSensors = new HashMap<>();
    private float mInitialStepCount = -1;
    private final float[] mAcceMeasure = new float[3];
    private final float[] mGyroMeasure = new float[3];
    private final float[] mMagnetMeasure = new float[3];

    private final float[] mAcceBias = new float[3];
    private final float[] mGyroBias = new float[3];
    private final float[] mMagnetBias = new float[3];

    private static final long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    private static final long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;
    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationListeningCallback callback = new LocationListeningCallback(this);

    /**
     * Set up the LocationEngine and the parameters for querying the device's location
     */
    @SuppressLint("MissingPermission")
    private void initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);

        LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();

        locationEngine.requestLocationUpdates(request, callback, Looper.getMainLooper());
        locationEngine.getLastLocation(callback);
    }

    @SuppressLint("MissingPermission")
    private void enableLocationComponent() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
initLocationEngine();
        } else {
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            // Permission sensitive logic called here, such as activating the Maps SDK's LocationComponent to show the device's location
            enableLocationComponent();
        } else {
            // User denied the permission
        }
    }


    private class LocationListeningCallback
            implements LocationEngineCallback<LocationEngineResult> {

        private final WeakReference<Service> fragmentWeakReference;

        LocationListeningCallback(CyClCoreService service) {
            this.fragmentWeakReference = new WeakReference<>(service);
        }

        @Override
        public void onSuccess(LocationEngineResult result) {

            // The LocationEngineCallback interface's method which fires when the device's location has changed.
            Location lastLocation = result.getLastLocation();
            if (lastLocation != null) {
                Log.e(TAG, "location: " + lastLocation.toString());
                if (lastLocation.hasAccuracy()) {
                    //TODO Send data to Interface.
                    Log.d(TAG, "Locations: "+ lastLocation.toString());
                    Intent intent = new Intent();
                    intent.setAction("VIVEK");
                    intent.putExtra("DATAPASSED", String.valueOf(lastLocation.getLatitude()));
                    intent.putExtra("ALBUM_DATA",String.valueOf(lastLocation.getLongitude()));
                    sendBroadcast(intent);

                }
            }
        }

        @Override
        public void onFailure(@NonNull Exception exception) {
            Log.e(TAG, "failed to get device location");

            // The LocationEngineCallback interface's method which fires when the device's location can not be captured
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: Service Invoked");
    }

    // methods
    public void registerSensors() {
        for (Sensor eachSensor : mSensors.values()) {
            mSensorManager.registerListener(this, eachSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    public void unregisterSensors() {
        for (Sensor eachSensor : mSensors.values()) {
            mSensorManager.unregisterListener(this, eachSensor);
        }
    }
       public void unregisterLocationEngine(){
           if (locationEngine != null) {
               locationEngine.removeLocationUpdates(callback);
           }
       }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterSensors();
        unregisterLocationEngine();
        Log.d(TAG, "onDestroy: Service");

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token));
        mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        mSensors.put("acce", mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        mSensors.put("acce_uncalib", mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER_UNCALIBRATED));
        mSensors.put("gyro", mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
        mSensors.put("gyro_uncalib", mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED));
        mSensors.put("linacce", mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION));
        mSensors.put("gravity", mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY));
        mSensors.put("magnet", mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
        mSensors.put("magnet_uncalib", mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED));
        mSensors.put("rv", mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR));
        mSensors.put("game_rv", mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR));
        mSensors.put("magnetic_rv", mSensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR));
        mSensors.put("step", mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER));
        mSensors.put("pressure", mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE));
        registerSensors();
        enableLocationComponent();
        return super.onStartCommand(intent, flags, startId);
    }

    public CyClCoreService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // set some variables
        float[] values = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};

        // update each sensor measurements
        long timestamp = sensorEvent.timestamp;
        Sensor eachSensor = sensorEvent.sensor;
        try {
            switch (eachSensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                    mAcceMeasure[0] = sensorEvent.values[0];
                    mAcceMeasure[1] = sensorEvent.values[1];
                    mAcceMeasure[2] = sensorEvent.values[2];
//                    if (isFileSaved) {
//                        mFileStreamer.addRecord(timestamp, "acce", 3, sensorEvent.values);
//                    }
                    Log.d(TAG, "onSensorChanged:acce "+String.valueOf(timestamp));
                    break;

                case Sensor.TYPE_ACCELEROMETER_UNCALIBRATED:
                    mAcceBias[0] = sensorEvent.values[3];
                    mAcceBias[1] = sensorEvent.values[4];
                    mAcceBias[2] = sensorEvent.values[5];
//                    if (isFileSaved) {
//                        mFileStreamer.addRecord(timestamp, "acce_uncalib", 3, sensorEvent.values);
//                        mFileStreamer.addRecord(timestamp, "acce_bias", 3, mAcceBias);
//                    }
                    Log.d(TAG, "onSensorChanged: acce_uncalib,acce_bias "+String.valueOf(timestamp));
                    break;

                case Sensor.TYPE_GYROSCOPE:
                    mGyroMeasure[0] = sensorEvent.values[0];
                    mGyroMeasure[1] = sensorEvent.values[1];
                    mGyroMeasure[2] = sensorEvent.values[2];
//                    if (isFileSaved) {
//                        mFileStreamer.addRecord(timestamp, "gyro", 3, sensorEvent.values);
//                    }
                    Log.d(TAG, "onSensorChanged:gyro "+String.valueOf(timestamp));
                    break;

                case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                    mGyroBias[0] = sensorEvent.values[3];
                    mGyroBias[1] = sensorEvent.values[4];
                    mGyroBias[2] = sensorEvent.values[5];
//                    if (isFileSaved) {
//                        mFileStreamer.addRecord(timestamp, "gyro_uncalib", 3, sensorEvent.values);
//                        mFileStreamer.addRecord(timestamp, "gyro_bias", 3, mGyroBias);
//                    }
                    Log.d(TAG, "onSensorChanged:gyro_uncalib,gyro_bias "+String.valueOf(timestamp));
                    break;

                case Sensor.TYPE_LINEAR_ACCELERATION:
//                    if (isFileSaved) {
//                        mFileStreamer.addRecord(timestamp, "linacce", 3, sensorEvent.values);
//                    }
                    Log.d(TAG, "onSensorChanged:linacce "+String.valueOf(timestamp));
                    break;

                case Sensor.TYPE_GRAVITY:
//                    if (isFileSaved) {
//                        mFileStreamer.addRecord(timestamp, "gravity", 3, sensorEvent.values);
//                    }
                    Log.d(TAG, "onSensorChanged:gravity "+String.valueOf(timestamp));
                    break;

                case Sensor.TYPE_MAGNETIC_FIELD:
                    mMagnetMeasure[0] = sensorEvent.values[0];
                    mMagnetMeasure[1] = sensorEvent.values[1];
                    mMagnetMeasure[2] = sensorEvent.values[2];
//                    if (isFileSaved) {
//                        mFileStreamer.addRecord(timestamp, "magnet", 3, sensorEvent.values);
//                    }
                    Log.d(TAG, "onSensorChanged:magnet "+String.valueOf(timestamp));
                    break;

                case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:
                    mMagnetBias[0] = sensorEvent.values[3];
                    mMagnetBias[1] = sensorEvent.values[4];
                    mMagnetBias[2] = sensorEvent.values[5];
//                    if (isFileSaved) {
//                        mFileStreamer.addRecord(timestamp, "magnet_uncalib", 3, sensorEvent.values);
//                        mFileStreamer.addRecord(timestamp, "magnet_bias", 3, mMagnetBias);
//                    }
                    Log.d(TAG, "onSensorChanged:magnet_uncalib,magnet_bias "+String.valueOf(timestamp));
                    break;

                case Sensor.TYPE_ROTATION_VECTOR:
//                    if (isFileSaved) {
//                        mFileStreamer.addRecord(timestamp, "rv", 4, sensorEvent.values);
//                    }
                    Log.d(TAG, "onSensorChanged:rv "+String.valueOf(timestamp));
                    break;

                case Sensor.TYPE_GAME_ROTATION_VECTOR:
//                    if (isFileSaved) {
//                        mFileStreamer.addRecord(timestamp, "game_rv", 4, sensorEvent.values);
//                    }
                    Log.d(TAG, "onSensorChanged:game_rv "+String.valueOf(timestamp));
                    break;

                case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
//                    if (isFileSaved) {
//                        mFileStreamer.addRecord(timestamp, "magnetic_rv", 4, sensorEvent.values);
//                    }
                    Log.d(TAG, "onSensorChanged:magnetic_rv "+String.valueOf(timestamp));
                    break;

                case Sensor.TYPE_STEP_COUNTER:
                    if (mInitialStepCount < 0) {
                        mInitialStepCount = sensorEvent.values[0] - 1;
                    }
                    values[0] = sensorEvent.values[0] - mInitialStepCount;
//                    if (isFileSaved) {
//                        mFileStreamer.addRecord(timestamp, "step", 1, values);
//                    }
                    Log.d(TAG, "onSensorChanged:step "+String.valueOf(timestamp));
                    break;

                case Sensor.TYPE_PRESSURE:
//                    if (isFileSaved) {
//                        mFileStreamer.addRecord(timestamp, "pressure", 1, sensorEvent.values);
//                    }
                    Log.d(TAG, "onSensorChanged:pressure "+String.valueOf(timestamp));
                    break;
            }
        } catch (Exception e) {
            Log.d(TAG, "onSensorChanged: Something is wrong.");
            e.printStackTrace();
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}