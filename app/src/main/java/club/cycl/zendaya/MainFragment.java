package club.cycl.zendaya;

import static com.mapbox.mapboxsdk.style.layers.Property.LINE_JOIN_ROUND;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleRadius;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineJoin;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;
import static com.mapbox.turf.TurfConstants.UNIT_KILOMETERS;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.hardware.SensorEvent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import club.cycl.zendaya.databinding.FragmentFirstBinding;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.UiSettings;
import com.mapbox.mapboxsdk.style.layers.CircleLayer;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.turf.TurfMeasurement;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;


public class MainFragment extends Fragment  implements
        OnMapReadyCallback, PermissionsListener {

    private static final String SOURCE_ID = "SOURCE_ID";
    private static final String CIRCLE_LAYER_ID = "CIRCLE_LAYER_ID";
    private static final String LINE_LAYER_ID = "LINE_LAYER_ID";

    // Adjust private static final variables below to change the example's UI
    private static final String STYLE_URI = "mapbox://styles/buradavivek/cl2kxo9fa003714l4j536pz0j";
    private static final int CIRCLE_COLOR = Color.RED;
    private static final int LINE_COLOR = CIRCLE_COLOR;
    private static final float CIRCLE_RADIUS = 6f;
    private static final float LINE_WIDTH = 4f;
    private static final String DISTANCE_UNITS = UNIT_KILOMETERS; // DISTANCE_UNITS must be equal to a String
    // found in the TurfConstants class

    private List<Point> pointList = new ArrayList<>();
    private MapView mapView;
    private MapboxMap mapboxMap;
    private TextView lineLengthTextView;
    private double totalLineDistance = 0;
    private boolean flag=true;

    private FragmentFirstBinding binding;
    private Context ctx;
    private String TAG =MainFragment.class.getName();

    private static final long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    private static final long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;
    HashMap<String , CyClPoint> point = new HashMap<>();
    HashMap<String , CyClSensor> sensordata = new HashMap<>();

    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private final MainFragmentLocationCallback callback =
            new MainFragmentLocationCallback(this);

    private CyClCoreService timerService;
    private boolean serviceBound;

    // Message type for the handler
    private final static int MSG_UPDATE_TIME = 0;
    private FirebaseFirestore db;

    @Subscribe
    public void HandleBackgroundPoints(CyClPoint event) {
        Log.i(TAG, "handleSomethingElse: "+event.toString());
        point.put(String.valueOf(System.currentTimeMillis())
                ,new CyClPoint(event.getLatitute(),event.getLongitude()));
        Log.e(TAG, "Size: "+point.size() );
        addClickPointToLine(new LatLng(event.latitute,event.getLongitude()));
    }

    @Subscribe
    public void HandleBackgroundPoints(CyClSensor event) {
        Log.e(TAG, "HandleBackgroundPoints: "+event.toString() );
        sensordata.put(String.valueOf(System.currentTimeMillis()),event);
    }

    @Override
    public View onCreateView(
             LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        Mapbox.getInstance(ctx, getString(R.string.mapbox_access_token));
         db = FirebaseFirestore.getInstance();

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        mapView = binding.mapView;
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        return binding.getRoot();
    }

    private void addClickPointToLine(@NonNull LatLng clickLatLng) {
        mapboxMap.getStyle(new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                // Get the source from the map's style
                GeoJsonSource geoJsonSource = style.getSourceAs(SOURCE_ID);
                if (geoJsonSource != null) {

                    pointList.add(Point.fromLngLat(clickLatLng.getLongitude(), clickLatLng.getLatitude()));

                    int pointListSize = pointList.size();

                    double distanceBetweenLastAndSecondToLastClickPoint = 0;

                    // Make the Turf calculation between the last tap point and the second-to-last tap point.
                    if (pointList.size() >= 2) {
                        distanceBetweenLastAndSecondToLastClickPoint = TurfMeasurement.distance(
                                pointList.get(pointListSize - 2), pointList.get(pointListSize - 1));
                    }

                    // Re-draw the new GeoJSON data
                    if (pointListSize >= 2 && distanceBetweenLastAndSecondToLastClickPoint > 0) {

                        // Add the last TurfMeasurement#distance calculated distance to the total line distance.
                        totalLineDistance += distanceBetweenLastAndSecondToLastClickPoint;

                        // Adjust the TextView to display the new total line distance.
                        // DISTANCE_UNITS must be equal to a String found in the TurfConstants class
                        binding.lineLength.setText(String.format(getString(R.string.line_distance_textview), DISTANCE_UNITS,
                                String.valueOf(Math.round(totalLineDistance))));

                        // Set the specific source's GeoJSON data
                        geoJsonSource.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(pointList)));
                    }
                }
            }
        });
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String session = UUID.randomUUID().toString();
                if(flag) {
                    requireActivity().startService(new Intent(ctx, CyClCoreService.class));
                    flag = false;
                    binding.button.setText("STOP");
                }
                else{
                    requireActivity().stopService(new Intent(ctx, CyClCoreService.class));
                    flag = true;
                    binding.button.setText("SAVING LOCATION");
                    binding.button.setClickable(false);
                    db.collection("users")
                            .document(session)
                            .collection("Location")
                            .add(point)
                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {
                                    Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                                    binding.button.setText("SAVING SENSORS");
                                    binding.button.setClickable(false);
                                    point.clear();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w(TAG, "Error adding document", e);
                                }
                            });

                    db.collection("users")
                            .document(session)
                            .collection("SensorData")
                            .add(sensordata)
                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {
                                    Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                                    sensordata.clear();
                                    binding.button.setText("START");
                                    binding.button.setClickable(true);
                                    binding.lineLength.setText(" ");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w(TAG, "Error adding document", e);
                                }
                            });

                }
            }
        });


        binding.buttonPermissions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", ctx.getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        ctx=context;
    }

    private static class MainFragmentLocationCallback
            implements LocationEngineCallback<LocationEngineResult> {

        private final WeakReference<MainFragment> activityWeakReference;

        MainFragmentLocationCallback(MainFragment activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location has changed.
         *
         * @param result the LocationEngineResult object which has the last known location within it.
         */
        @Override
        public void onSuccess(LocationEngineResult result) {
            MainFragment activity = activityWeakReference.get();

            if (activity != null) {
                Location location = result.getLastLocation();
                if (location == null) {
                    return;
                }
                // Pass the new location to the Maps SDK's LocationComponent
                if (activity.mapboxMap != null && result.getLastLocation() != null) {
                    activity.mapboxMap.getLocationComponent().forceLocationUpdate(result.getLastLocation());
                }
            }
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location can't be captured
         *
         * @param exception the exception message
         */
        @Override
        public void onFailure(@NonNull Exception exception) {
            MainFragment activity = activityWeakReference.get();
            if (activity != null) {
                Log.i("MainFragment", "onFailure: ");
                Toast.makeText(activity.ctx, exception.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
        if(!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
        Log.d(TAG, "onStop: MAinActivity Stopped");
    }


    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
        // Prevent leaks
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates(callback);
        }
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(ctx, "onExplanation Needed",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            mapboxMap.getStyle(new Style.OnStyleLoaded() {
                @Override
                public void onStyleLoaded(@NonNull Style style) {
                    enableLocationComponent(style);
                    UiSettings uiSettings = mapboxMap.getUiSettings();
                }
            });
        } else {

            Toast.makeText(ctx, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;


        mapboxMap.setStyle(new Style.Builder().fromUri("mapbox://styles/buradavivek/cl2kxo9fa003714l4j536pz0j"),
                new Style.OnStyleLoaded() {
                    @Override public void onStyleLoaded(@NonNull Style style) {
                        enableLocationComponent(style);
                    }
                });

        mapboxMap.setStyle(new Style.Builder()
                        .fromUri(STYLE_URI)

                        // Add the source to the map
                        .withSource(new GeoJsonSource(SOURCE_ID))

                        // Style and add the CircleLayer to the map
                        .withLayer(new CircleLayer(CIRCLE_LAYER_ID, SOURCE_ID).withProperties(
                                circleColor(CIRCLE_COLOR),
                                circleRadius(CIRCLE_RADIUS)
                        ))

                        // Style and add the LineLayer to the map. The LineLayer is placed below the CircleLayer.
                        .withLayerBelow(new LineLayer(LINE_LAYER_ID, SOURCE_ID).withProperties(
                                lineColor(LINE_COLOR),
                                lineWidth(LINE_WIDTH),
                                lineJoin(LINE_JOIN_ROUND)
                        ), CIRCLE_LAYER_ID), new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        enableLocationComponent(style);
                    }
                }
        );
    }

    /**
     * Initialize the Maps SDK's LocationComponent
     */
    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(ctx)) {

            // Get an instance of the component
            LocationComponent locationComponent = mapboxMap.getLocationComponent();

            // Set the LocationComponent activation options
            LocationComponentActivationOptions locationComponentActivationOptions =
                    LocationComponentActivationOptions.builder(ctx, loadedMapStyle)
                            .useDefaultLocationEngine(false)
                            .build();

            // Activate with the LocationComponentActivationOptions object
            locationComponent.activateLocationComponent(locationComponentActivationOptions);

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

            // Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);

            initLocationEngine();
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(requireActivity());
        }
    }


    /**
     * Set up the LocationEngine and the parameters for querying the device's location
     */
    @SuppressLint("MissingPermission")
    private void initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(ctx);

        LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();

        locationEngine.requestLocationUpdates(request, callback, Looper.getMainLooper());
        locationEngine.getLastLocation(callback);
    }

}