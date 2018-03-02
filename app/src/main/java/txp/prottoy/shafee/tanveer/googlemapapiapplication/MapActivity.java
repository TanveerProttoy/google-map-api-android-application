package txp.prottoy.shafee.tanveer.googlemapapiapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback,
        OnCompleteListener<Location> {
    private static final int DIALOG_REQUEST_ERROR = 999;
    private static final int LOCATION_REQUEST_CODE = 99;
    private static final float DEFAULT_ZOOM = 15f;
    private static final int PERMISSION_GRANTED_CODE = PackageManager.PERMISSION_GRANTED;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private EditText editTextMap;
    private boolean isLocationPermissionGranted;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private ImageButton gpsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        initializeUIComponents();
        if(serviceAvailable()) {
            getLocationPermission();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        switch(requestCode) {
            case LOCATION_REQUEST_CODE: {
                if(grantResults.length > 0) {
                    for(int grantResult : grantResults) {
                        if(grantResult != PERMISSION_GRANTED_CODE) {
                            isLocationPermissionGranted = false;
                            return;
                        }
                    }
                    isLocationPermissionGranted = true;
                    initializeMap();
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        if(isLocationPermissionGranted) {
            getCurrentLocation();
            try {
                this.googleMap.setMyLocationEnabled(true);
                this.googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            }
            catch(SecurityException e) {
                showToastMessage("Could not set current location on Map," +
                        "please provide required permissions");
            }
        }
    }

    @Override
    public void onComplete(@NonNull Task<Location> task) {
        try {
            if(task.isSuccessful()) {
                Location currentLocation = task.getResult();
                moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                        DEFAULT_ZOOM, "My location");
                return;
            }
            showToastMessage("Could not get current location");
        }
        catch(NullPointerException n) {
            showToastMessage("Could not get current location");
        }
    }

    private void initializeUIComponents() {
        editTextMap = findViewById(R.id.edit_text_map);
        gpsButton = findViewById(R.id.gps_btn_map);
        editTextMap.setOnEditorActionListener(editTextActionListener);
        gpsButton.setOnClickListener(gpsOnClickListener);
    }

    private boolean serviceAvailable() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int result = googleApiAvailability
                .isGooglePlayServicesAvailable(MapActivity.this);
        if(result == ConnectionResult.SUCCESS) {
            return true;
        }
        else if(googleApiAvailability.isUserResolvableError(result)) {
            googleApiAvailability.getErrorDialog(MapActivity.this,
                    result, DIALOG_REQUEST_ERROR);
            return false;
        }
        return false;
    }

    private void getLocationPermission() {
        if((ContextCompat.checkSelfPermission(getApplicationContext(),
                FINE_LOCATION) == PERMISSION_GRANTED_CODE)
                && (ContextCompat.checkSelfPermission(getApplicationContext(),
                COARSE_LOCATION) == PERMISSION_GRANTED_CODE)) {
            isLocationPermissionGranted = true;
            initializeMap();
        }
        else {
            ActivityCompat.requestPermissions(MapActivity.this,
                    new String[] {FINE_LOCATION, COARSE_LOCATION}, LOCATION_REQUEST_CODE);
        }
    }

    private void initializeMap() {
        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        supportMapFragment.getMapAsync(MapActivity.this);
    }

    private void getCurrentLocation() {
        try {
            fusedLocationProviderClient = LocationServices
                    .getFusedLocationProviderClient(MapActivity.this);
            Task<Location> task = fusedLocationProviderClient.getLastLocation();
            task.addOnCompleteListener(MapActivity.this);
        }
        catch(SecurityException s) {

        }
    }

    private void moveCamera(LatLng latLng, float zoom, String title) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        googleMap.addMarker(new MarkerOptions().position(latLng).title(title));
    }

    private void locateGeo() {
        List<Address> addresses = new ArrayList<>();
        Geocoder geocoder = new Geocoder(MapActivity.this);
        try {
            addresses = geocoder.getFromLocationName(editTextMap.getText().toString(), 1);
        }
        catch(IOException i) {

        }
        if(addresses.size() > 0) {
            Address address = addresses.get(0);
            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), DEFAULT_ZOOM,
                    address.getAddressLine(0));
        }
    }

    private void showToastMessage(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private EditText.OnEditorActionListener editTextActionListener
            = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
            if(actionId == EditorInfo.IME_ACTION_SEARCH) {
                locateGeo();
                return true;
            }
            return false;
        }
    };

    private View.OnClickListener gpsOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            getCurrentLocation();
        }
    };
}
