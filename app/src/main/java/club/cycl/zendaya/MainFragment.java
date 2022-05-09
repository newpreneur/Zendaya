package club.cycl.zendaya;

import static com.mapbox.mapboxsdk.style.layers.Property.LINE_JOIN_ROUND;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.circleRadius;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineJoin;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;
import static com.mapbox.turf.TurfConstants.UNIT_KILOMETERS;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.SensorEvent;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import club.cycl.zendaya.databinding.FragmentFirstBinding;
import kotlin.Unit;
import se.warting.permissionsui.backgroundlocation.PermissionsUiContracts;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
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
        OnMapReadyCallback {

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
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
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

    private static final int RC_SIGN_IN = 9001;

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    private GoogleSignInClient mGoogleSignInClient;

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

    @SuppressLint("SetTextI18n")
    ActivityResultLauncher<Unit> mGetContent = registerForActivityResult(
            new PermissionsUiContracts.RequestBackgroundLocation(),
            success -> {
                mapboxMap.getStyle(new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        enableLocationComponent(style);
                    }
                });
            }
    );

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
//                        binding.lineLength.setText(String.format(getString(R.string.line_distance_textview), DISTANCE_UNITS,
//                                String.valueOf(Math.round(totalLineDistance))));

                        // Set the specific source's GeoJSON data
                        geoJsonSource.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(pointList)));
                    }
                }
            }
        });
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w(TAG, "Google sign in failed", e);
                updateUI(null);
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Snackbar.make(binding.getRoot(), "Authentication Failed.", Snackbar.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    }
                });
    }



    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signOut() {
        // Firebase sign out
        mAuth.signOut();

        // Google sign out
        mGoogleSignInClient.signOut().addOnCompleteListener(requireActivity(),
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        updateUI(null);
                    }
                });
    }

    private void revokeAccess() {
        // Firebase sign out
        mAuth.signOut();

        // Google revoke access
        mGoogleSignInClient.revokeAccess().addOnCompleteListener(requireActivity(),
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        updateUI(null);
                    }
                });
    }

    private void updateUI(FirebaseUser user) {

        if (user != null) {

        } else {

        }
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

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
                    binding.button.setText("SAVING DATA");
                    binding.button.setClickable(false);
                    WriteBatch batch = db.batch();
                    DocumentReference locationRef = db.collection(session).document("LocationData");
                    batch.set(locationRef,point);
//                    CollectionReference sensorRef = db.collection("latest").document(session).collection("SensorData");
                    DocumentReference sensorRef =db.collection(session).document("SensorData");
                    batch.set(sensorRef,sensordata);
                    batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            binding.button.setText("START");
                            binding.button.setClickable(true);
                            point.clear();
                            sensordata.clear();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(ctx,"Data Saving failed",Toast.LENGTH_LONG).show();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(ctx,"Data Saving completed!",Toast.LENGTH_LONG).show();
                        }
                    });

                }
            }
        });


        binding.buttonPermissions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                Uri uri = Uri.fromParts("package", ctx.getPackageName(), null);
//                intent.setData(uri);
//                startActivity(intent);
                signIn();
            }
        });
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
    public void onStart() {
        super.onStart();
        mapView.onStart();
        if(!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
        // Check if user is signed in (non-null) and update UI accordingly.
//        FirebaseUser currentUser = mAuth.getCurrentUser();
//        updateUI(currentUser);

    }

    /**
    // You can do the assignment inside onAttach or onCreate, i.e, before the activity is displayed
    ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent data = result.getData();

                    }
                }
            });

     **/




    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;

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
    @SuppressLint("MissingPermission")
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

            mGetContent.launch(null);
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


    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
        Log.d(TAG, "onStop: MainActivity Stopped");
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
        if (ContextCompat.checkSelfPermission(ctx,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
        }

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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
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

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }



}