package sapadvrtisrproj.ms.sapientia.ro.allam4;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.UUID;

import sapadvrtisrproj.ms.sapientia.ro.allam4.Data.User;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION = 1;
    private static final String TAG = "MainActivity";
    private Button button, buttonTrack;
    private TextView textView;
    private LocationManager locationManager;
    private String latitude, longitude;
    private FirebaseFirestore mFirestore;
    private User newUser;
    private boolean coordinatesFound = false;
    //
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFirestore = FirebaseFirestore.getInstance();

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);

        textView = findViewById(R.id.text_location);
        button = findViewById(R.id.button_location);
        buttonTrack = findViewById(R.id.trackButton);


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    buildAlertMessageNoGps();
                } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Log.d(TAG, "location access is granted");
                    coordinatesFound = getLocation();

                    if(coordinatesFound) {
                        Log.d(TAG, "coordinates were found! Uploading to database...");
                        uploadCurrentUserCoordinates();
                    }
                }
            }
        });

        buttonTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, TrackActivity.class));
            }
        });
    }


    private boolean getLocation() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (location != null) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();

                latitude = String.valueOf(lat);
                longitude = String.valueOf(lon);

                textView.setText("Your current location is\nLatitude: " + latitude + "\nLongitude: " + longitude);

                return true;
            } else {
                Toast.makeText(this, "Unable to trace your location", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return false;
    }

    protected void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Please turn on your GPS")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private void uploadCurrentUserCoordinates() {
        newUser = new User("0", latitude, longitude, "asd", com.google.firebase.Timestamp.now());
        mFirestore.collection("userCoordinates").add(newUser).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                Toast.makeText(MainActivity.this, "User data uploaded to the database.", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "location data uploaded");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                String error = e.getMessage();
                Toast.makeText(MainActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "location data couldn't be uploaded");
            }
        });
    }
}
