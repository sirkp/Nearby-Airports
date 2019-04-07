package com.example.nearbyairports;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nearbyairports.model.findplace.PlacesResponse;
import com.example.nearbyairports.model.textsearch.TextSearchResponse;
import com.example.nearbyairports.service.RetrofitMaps;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;
    double latitude;
    double longitude;
    private int PROXIMITY_RADIUS = 100000;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    LocationRequest mLocationRequest;
    private String key;
    private LinearLayout layoutBottomSheet;
    private TextView tvName;
    private TextView tvVicinity;
    private BottomSheetBehavior sheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        key = getResources().getString(R.string.google_maps_key);
        //show error dialog if Google Play Services not available
        if (!isGooglePlayServicesAvailable()) {
            Log.d("onCreate", "Google Play Services not available. Ending Test case.");
            finish();
        } else {
            Log.d("onCreate", "Google Play Services available. Continuing.");
        }

        if (key.length() == 0) {
            Log.d("onCreate", "API key missing. Ending Test case.");
            finish();
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Button btnNearby = findViewById(R.id.btnNearbyAirports);
        btnNearby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nearbySearch("airport");
            }
        });

        layoutBottomSheet = findViewById(R.id.bottom_sheet);
        tvName = layoutBottomSheet.findViewById(R.id.tv_location_name);
        tvVicinity = layoutBottomSheet.findViewById(R.id.tv_vicinity);
        sheetBehavior = BottomSheetBehavior.from(layoutBottomSheet);
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int result = googleAPI.isGooglePlayServicesAvailable(this);
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                googleAPI.getErrorDialog(this, result,
                        0).show();
            }
            return false;
        }
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
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        } else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {

        Log.d("onLocationChanged", "entered");

        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }
        //Place current location marker
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");

        // Adding colour to the marker
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));

        // Adding Marker to the Map
        mCurrLocationMarker = mMap.addMarker(markerOptions);

        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

        Log.d("onLocationChanged", String.format("latitude:%.3f longitude:%.3f", latitude, longitude));

        Log.d("onLocationChanged", "Exit");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            // You can add here other case statements according to your requirement.
        }
    }

    private RetrofitMaps buildRetrofitService() {
        String url = "https://maps.googleapis.com/maps/";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(RetrofitMaps.class);
    }

    private void textSearch(String query) {
        RetrofitMaps service = buildRetrofitService();
        Call<TextSearchResponse> call = service.textSearch("airport", query, 2000, latitude + "," + longitude, key);

        call.enqueue(new Callback<TextSearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<TextSearchResponse> call, @NonNull Response<TextSearchResponse> response) {

                try {
                    mMap.clear();
                    if (response.body() != null && response.body().getResults() != null) {
                        if (response.body().getResults().size() > 0) {
                            Double lat = response.body().getResults().get(0).getGeometry().getLocation().getLat();
                            Double lng = response.body().getResults().get(0).getGeometry().getLocation().getLng();
                            String placeName = response.body().getResults().get(0).getName();
                            String vicinity = response.body().getResults().get(0).getVicinity();
                            MarkerOptions markerOptions = new MarkerOptions();
                            LatLng latLng = new LatLng(lat, lng);
                            // Position of Marker on Map
                            markerOptions.position(latLng);
                            // Adding Title to the Marker
                            markerOptions.title(placeName + " : " + vicinity);
                            tvName.setText(placeName);
                            tvVicinity.setText(vicinity);
                            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                            // Adding Marker to the Camera.
                            Marker m = mMap.addMarker(markerOptions);
                            // Adding colour to the marker
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                            // move map camera
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                            mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
                        }
                    }
                } catch (Exception e) {
                    Log.d("onResponse", "There is an error");
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(@NonNull Call<TextSearchResponse> call, Throwable t) {
                Log.d("onFailure", t.toString());
            }
        });
    }

    private void findPlaces(String input) {
        RetrofitMaps service = buildRetrofitService();
        Call<PlacesResponse> call = service.findPlaceFromText(input, 2000, latitude, longitude, key);

        call.enqueue(new Callback<PlacesResponse>() {
            @Override
            public void onResponse(@NonNull Call<PlacesResponse> call, @NonNull Response<PlacesResponse> response) {

                try {
                    mMap.clear();
                    // This loop will go through all the results and add marker on each location.
                    if (response.body() != null && response.body().getCandidates() != null) {
                        if (response.body().getCandidates().size() > 0) {
                            Double lat = response.body().getCandidates().get(0).getGeometry().getLocation().getLat();
                            Double lng = response.body().getCandidates().get(0).getGeometry().getLocation().getLng();
                            String placeName = response.body().getCandidates().get(0).getName();
                            String vicinity = response.body().getCandidates().get(0).getFormattedAddress();
                            MarkerOptions markerOptions = new MarkerOptions();
                            LatLng latLng = new LatLng(lat, lng);
                            // Position of Marker on Map
                            markerOptions.position(latLng);
                            // Adding Title to the Marker
                            markerOptions.title(placeName + " : " + vicinity);
                            tvName.setText(placeName);
                            tvVicinity.setText(vicinity);
                            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                            // Adding Marker to the Camera.
                            Marker m = mMap.addMarker(markerOptions);
                            // Adding colour to the marker
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                            // move map camera
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                            mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
                        }
                    }
                } catch (Exception e) {
                    Log.d("onResponse", "There is an error");
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(@NonNull Call<PlacesResponse> call, Throwable t) {
                Log.d("onFailure", t.toString());
            }
        });
    }

    private void nearbySearch(String query) {
        RetrofitMaps service = buildRetrofitService();
        Call<TextSearchResponse> call = service.nearbySearch("airport", query, 12000, latitude + "," + longitude, key);

        call.enqueue(new Callback<TextSearchResponse>() {
            @Override
            public void onResponse(@NonNull Call<TextSearchResponse> call, @NonNull Response<TextSearchResponse> response) {

                try {
                    mMap.clear();
                    // This loop will go through all the results and add marker on each location.
                    if (response.body() != null && response.body().getResults() != null) {
                        if (response.body().getResults().size() > 0) {
                            Double lat = response.body().getResults().get(0).getGeometry().getLocation().getLat();
                            Double lng = response.body().getResults().get(0).getGeometry().getLocation().getLng();
                            String placeName = response.body().getResults().get(0).getName();
                            String vicinity = response.body().getResults().get(0).getVicinity();
                            MarkerOptions markerOptions = new MarkerOptions();
                            LatLng latLng = new LatLng(lat, lng);
                            // Position of Marker on Map
                            markerOptions.position(latLng);
                            // Adding Title to the Marker
                            markerOptions.title(placeName + " : " + vicinity);
                            tvName.setText(placeName);
                            tvVicinity.setText(vicinity);
                            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                            // Adding Marker to the Camera.
                            Marker m = mMap.addMarker(markerOptions);
                            // Adding colour to the marker
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
                            // move map camera
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                            mMap.animateCamera(CameraUpdateFactory.zoomTo(11));
                        }
                    }
                } catch (Exception e) {
                    Log.d("onResponse", "There is an error");
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(@NonNull Call<TextSearchResponse> call, Throwable t) {
                Log.d("onFailure", t.toString());
            }
        });
    }
}
