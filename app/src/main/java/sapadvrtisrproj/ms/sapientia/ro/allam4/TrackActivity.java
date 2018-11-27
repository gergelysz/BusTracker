package sapadvrtisrproj.ms.sapientia.ro.allam4;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import sapadvrtisrproj.ms.sapientia.ro.allam4.Data.Station;
import sapadvrtisrproj.ms.sapientia.ro.allam4.Data.User;

public class TrackActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private static final int REQUEST_LOCATION = 1234;
    private static final String TAG = "TrackActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private String latitude, longitude;
    private Boolean mLocationPermissionGranted = false;
    private FirebaseFirestore mFirestore;
    private User newUser;
    private String userId = null;
    private boolean coordinatesFound = false;
    private ArrayList<Station> busStations = new ArrayList<>();
    private ArrayList<User> usersList = new ArrayList<>();
    private double closest = 2000;
    private static final CharSequence[] statusTypes = {"on bus", "waiting for bus"};
    //    private Station saveStation = null;
    private String closestStationName = "";
    private List<Marker> allUsersMarker = new ArrayList<>();

    private String userBus = "0", userStatus = "waiting for bus";

    private Marker currentUserMarker = null;

    private TextView statusHeaderNavBar;
    private TextView closestStationHeaderNavBar;

    @Override
    protected void onDestroy() {
        /**
         *   Stopping listening to location data and
         *   deleting user's location data from database
         */
        locationManager.removeUpdates(locationListener);
        locationManager = null;
        if (userId != null) {
            mFirestore.collection("userCoordinates").document(userId).delete();
        }
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        statusHeaderNavBar = findViewById(R.id.status_nav_header);
        closestStationHeaderNavBar = findViewById(R.id.closest_station_nav_header);

        mFirestore = FirebaseFirestore.getInstance();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        /**
         *   1. Getting location permission
         *   2. Getting stations data and showing them on the map
         *   3. Getting users data and showing them on the map
         *   4. Check and set user's status: 'waiting for bus' or 'on bus'
         */

        getLocationPermission();
        setStations();
        getUsersData();
        checkAndSetUserStatus();


        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d(TAG, "Location change: lat=" + location.getLatitude() + ", lon=" + location.getLongitude());
                LatLng currentLocationLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                if (userId == null) {
                    currentUserMarker = mMap.addMarker(new MarkerOptions()
                            .position(currentLocationLatLng)
                            .title("Current position")
                            .draggable(false));
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocationLatLng, 16));
                } else {
                    currentUserMarker.setPosition(currentLocationLatLng);
                    updateLocationInDatabase();
                }

                getUsersData();

                for (Station station : busStations) {
                    Location locationStation = new Location("asd");
                    locationStation.setLatitude(Double.parseDouble(station.getLatitude()));
                    locationStation.setLongitude(Double.parseDouble(station.getLongitude()));


                    if (closest > location.distanceTo(locationStation)) {
                        closest = location.distanceTo(locationStation);
                        closestStationName = station.getName();
                    }
                }

//                Toast.makeText(TrackActivity.this, "Closest station:\n" + closestStationName, Toast.LENGTH_LONG).show();
                closestStationHeaderNavBar.setText(R.id.closest_station_nav_header + closestStationName);


            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        Log.d(TAG, "requesting location updates: starting");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                5000,
                0,
                locationListener);

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                0,
                0,
                locationListener);
        Log.d(TAG, "requesting location updates: success");

        coordinatesFound = getLocation();
        if (coordinatesFound) {
            uploadCurrentUserData();
        }


    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.track, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_select_station) {

        } else if (id == R.id.nav_offline_bus_data) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);


        /**
         *  Loading maps style
         */

        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.map_style_dark_1));

            if (!success) {
                Log.e(TAG, "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }
    }

    private void initMap() {

        /**
         *   Initializes map fragment
         */

        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(TrackActivity.this);
    }

    private boolean getLocation() {

        /**
         *   If the location listener get's the last
         *   location, it stores the latitude and longitude
         *   else it notifies the user with a Toast message
         */

        if (ActivityCompat.checkSelfPermission(TrackActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(TrackActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {

            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (location != null) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();

                latitude = String.valueOf(lat);
                longitude = String.valueOf(lon);

                return true;

            } else {
                Toast.makeText(this, "Unable to trace your location", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return false;
    }

    private void getLocationPermission() {

        /**
         *   Checks if location permission is granted,
         *   if it is, it initializes the map, if not,
         *   it asks for permission
         */

        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this.getApplicationContext(),
                        COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            initMap();
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    REQUEST_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called");
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case REQUEST_LOCATION:
                if (grantResults.length > 0 /*&& grantResults[0] == PackageManager.PERMISSION_GRANTED*/) {
                    for (int i = 0; i < grantResults.length; ++i) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionGranted = true;
                    Log.d(TAG, "initMap call (initializing map)");
                    initMap();
                }
                break;
        }
    }

    private void updateLocationInDatabase() {

        /**
         *   Updating location in database.
         *   First updating the local User variable, then resetting
         *   it's values in the database.
         */

        newUser.setBus(userBus);
        newUser.setStatus(userStatus);
        newUser.setLatitude(latitude);
        newUser.setLongitude(longitude);
        newUser.setTimestamp(Timestamp.now());

        mFirestore.collection("userCoordinates").document(userId).set(newUser).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "Current user's (" + userId + ") location data updated");
            }
        });
    }

    private void setStations() {

        /**
         *   Setting stations from database
         *   and drawing them with markers on map
         */

        Log.d(TAG, "setStations function called");

        mFirestore.collection("stationsCoordinates").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    Station station = new Station(
                            documentSnapshot.getString("latitude"),
                            documentSnapshot.getString("longitude"),
                            documentSnapshot.getString("name")
                    );
                    Log.d(TAG, "reading bus station data: " + station.getName());
                    busStations.add(station);
                }

                if (busStations.size() != 0) {

                    for (Station station : busStations) {

                        Log.d(TAG, "creating bus stations markers");

                        LatLng latLng = new LatLng(Double.parseDouble(station.getLatitude()), Double.parseDouble(station.getLongitude()));

                        mMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .title(station.getName())
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_bus_station))
                                .draggable(false)
                        );
                    }
                }
            }
        });
    }

    private void uploadCurrentUserData() {

        /**
         *   Creates user and uploads
         *   first data
         */

        newUser = new User(userBus, latitude, longitude, userStatus, com.google.firebase.Timestamp.now());
        mFirestore.collection("userCoordinates").add(newUser).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                Toast.makeText(TrackActivity.this, "User data uploaded to the database.", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "location data uploaded");
                userId = documentReference.getId();
//                Toast.makeText(TrackActivity.this, "Current user's id: " + userId, Toast.LENGTH_SHORT).show();

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                String error = e.getMessage();
                Toast.makeText(TrackActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "location data couldn't be uploaded");
            }
        });
    }

    private void getUsersData() {
        /**
         *   Getting data from database
         *   and drawing user on map with marker
         */

        if (!allUsersMarker.isEmpty()) {
            for (int i = 0; i < allUsersMarker.size(); ++i) {
                allUsersMarker.get(i).remove();
            }
        }

        Log.d(TAG, "Getting users data");

        mFirestore.collection("userCoordinates").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                    User user = new User(
                            documentSnapshot.getString("bus"),
                            documentSnapshot.getString("latitude"),
                            documentSnapshot.getString("longitude"),
                            documentSnapshot.getString("status"),
                            documentSnapshot.getTimestamp("timestamp")
                    );
                    Log.d(TAG, "Reading user's data: " + documentSnapshot.getId() + " " + user.getStatus() + " " + user.getBus());
                    usersList.add(user);

                    LatLng latLng = new LatLng(Double.parseDouble(user.getLatitude()), Double.parseDouble(user.getLongitude()));
                    Marker m = mMap.addMarker(new MarkerOptions().title(documentSnapshot.getId()).position(latLng).icon(BitmapDescriptorFactory.fromResource(R.drawable.bus)));
                    allUsersMarker.add(m);
                }
            }
        });
    }

    private void checkAndSetUserStatus() {

        /**
         *   Dialog to set user's 'status'
         *   'on bus' or 'waiting for bus'
         */

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Please set your status")
                .setItems(statusTypes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                Toast.makeText(TrackActivity.this, "on bus", Toast.LENGTH_LONG).show();
                                userBus = "on bus";
                                statusHeaderNavBar.setText(R.id.status_nav_header + userBus);
                                mFirestore.collection("userCoordinates").document(userId).update("status", "on bus").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Snackbar.make(findViewById(R.id.map), R.string.user_status_update_success, Snackbar.LENGTH_SHORT).show();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Snackbar.make(findViewById(R.id.map), R.string.user_status_update_failure, Snackbar.LENGTH_SHORT).show();
                                    }
                                });
                                break;
                            case 1:
                                Toast.makeText(TrackActivity.this, "waiting for bus", Toast.LENGTH_LONG).show();
                                userBus = "waiting for bus";
                                statusHeaderNavBar.setText(R.id.status_nav_header + userBus);
                                mFirestore.collection("userCoordinates").document(userId).update("status", "waiting for bus").addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Snackbar.make(findViewById(R.id.map), R.string.user_status_update_success, Snackbar.LENGTH_SHORT).show();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Snackbar.make(findViewById(R.id.map), R.string.user_status_update_failure, Snackbar.LENGTH_SHORT).show();
                                    }
                                });
                                break;
                        }
                    }
                });
        builder.create().show();
    }
}
