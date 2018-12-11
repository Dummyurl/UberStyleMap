package dev.uberstyle.map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends FragmentActivity implements ActivityCompat.OnRequestPermissionsResultCallback,
        OnMapReadyCallback, GoogleMap.OnCameraChangeListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private String TAG = MainActivity.class.getSimpleName();

    private GoogleMap mMap;
    private LatLng origin;
    private LatLng dest;
    private double mYLatitude = 0.0;
    private double mYLongitude = 0.0;

    private static final long INTERVAL = 1000 * 10;
    private static final long FASTEST_INTERVAL = 1000 * 5;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    private GoogleApiClient mGoogleApiClient;
    private Boolean isStarted = false;
    private Boolean isNavigationStarted = false;
    private Boolean isNavigate = true;
    private String address = "";
    private ProgressDialog mProgressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        if (!NetworkUtils.isNetworkConnected(this)) {
            showMessage("Network Not Connected");
            return;
        }

        setUpMap();
    }

    @OnClick(R.id.fab)
    void onGetCurrentLocationClick() {
        Boolean isPermission = MarshmallowPermissions.checkPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (!isPermission) {
            MarshmallowPermissions.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
            return;
        }

        if (mMap != null) {
            mMap.clear();
            stopAnim();
            getCurrentLocation();
        }
    }

    @OnClick(R.id.btnWhereToGo)
    void onWhereToGoClick() {
        Boolean isPermission = MarshmallowPermissions.checkPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (!isPermission) {
            MarshmallowPermissions.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
            return;
        }

        Intent intent = new Intent(this, WhereToGo.class);
        intent.putExtra("address", address);
        intent.putExtra("latitude", mYLatitude);
        intent.putExtra("longitude", mYLongitude);
        startActivityForResult(intent, 100);
    }

    private void setUpMap() {
        Boolean isPermission = MarshmallowPermissions.checkPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (!isPermission) {
            MarshmallowPermissions.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
            return;
        }

        checkGoogleClientOptions();
        showLoading();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void checkGoogleClientOptions() {
        if (!isGooglePlayServicesAvailable()) {
            return;
        }

        createLocationRequest();
        mGoogleApiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API)
                .addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onStart() {
        super.onStart();
//        if (mGoogleApiClient != null) {
//            mGoogleApiClient.connect();
//        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
    }

    protected void startLocationUpdates() {
        @SuppressLint("MissingPermission") PendingResult<Status> pendingResult = LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "Connection failed: " + connectionResult.toString());
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        if (!isStarted) {
            isStarted = true;
            getCurrentLocation();
        }

        if (isNavigate) {
            //startRide(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
        }
    }

    private void getCurrentLocation() {
        if (null != mCurrentLocation) {
            mYLatitude = mCurrentLocation.getLatitude();
            mYLongitude = mCurrentLocation.getLongitude();
            setCurrentLocationMarkerOnMap(new LatLng(mYLatitude, mYLongitude));
            getLocationFromMyLatLong();
        }
    }

    @SuppressLint("SetTextI18n")
    public void getLocationFromMyLatLong() {
        List<Address> addresses;
        Geocoder coder = new Geocoder(this, Locale.getDefault());
        try {
            addresses = coder.getFromLocation(mYLatitude, mYLongitude, 1);

            Address location = addresses.get(0);

            address = location.getLocality();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    protected void stopLocationUpdates() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setCompassEnabled(false);
        mMap.setBuildingsEnabled(true);
        mMap.setMyLocationEnabled(false);
        mMap.setOnCameraChangeListener(this);
        boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json));

        if (!success) {
            Log.e(TAG, "Style parsing failed.");
        }
    }

    private void setMarkerOnMap(LatLng origin, LatLng dest) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        Marker sourceMarker = mMap.addMarker(new MarkerOptions().position(origin).title("Source").draggable(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.user_marker)));
        sourceMarker.setDraggable(true);
        builder.include(sourceMarker.getPosition());

        Marker destinationMarker = mMap.addMarker(new MarkerOptions().position(dest).draggable(true).title("Destination").icon(BitmapDescriptorFactory.fromResource(R.drawable.provider_marker)));
        destinationMarker.setDraggable(true);
        builder.include(destinationMarker.getPosition());

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 550, 550, 20));
        mMap.getUiSettings().setMapToolbarEnabled(false);
    }

    private void setCurrentLocationMarkerOnMap(LatLng latLng) {
        Marker sourceMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Current Location")
                .draggable(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.current_location)));
        sourceMarker.setDraggable(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        // For zooming automatically to the location of the marker
        CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(16.0f).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), new GoogleMap.CancelableCallback() {
            @Override
            public void onFinish() {
                hideLoading();
            }

            @Override
            public void onCancel() {
                hideLoading();
            }
        });
    }

    private void startRide(LatLng latLng) {
        Marker sourceMarker = null;

        sourceMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Car Move")
                .draggable(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.provider_location_icon)));
        sourceMarker.setDraggable(false);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(sourceMarker.getPosition());

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 200, 200, 20));
    }

    private void stopNavigation() {
        if (mMap != null) {
            mMap.clear();
        }
        isNavigate = false;
    }

    private void startNavigation() {
        if (mMap != null) {
            mMap.clear();
        }
        isNavigate = true;
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

    }

    private void startAnim(ArrayList<LatLng> arrayList) {
        if (this.mMap == null || arrayList.size() <= 1) {
            showMessage("Map not ready");
        } else {
            MapAnimator.getInstance().animateRoute(mMap, arrayList);
        }
    }

    private void stopAnim() {
        if (this.mMap != null) {
            MapAnimator.getInstance().stopAnim();
        } else {
            showMessage("Map not ready");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 100: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setUpMap();
                } else {
                    Log.e(TAG, "Permission Not Granted");
                    showMessage(getString(R.string.allow_storage_permission));
                }

                break;
            }
        }
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";

        String key = "key=" + getResources().getString(R.string.google_map_api);

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + key;

        // Output format
        String output = "json";

        // Building the url to the web service
        return "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
    }

    /**
     * A method to download json data from url
     */
    @SuppressLint("LongLogTag")
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuilder sb = new StringBuilder();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.e("Exception while downloading url", e.toString());
        } finally {
            if (iStream != null) {
                iStream.close();
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return data;
    }

    // Fetches data from url passed
    @SuppressLint("StaticFieldLeak")
    private class DownloadTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.e("Background Task", e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    @SuppressLint("StaticFieldLeak")
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            hideLoading();

            ArrayList<LatLng> points = new ArrayList<>();

            PolylineOptions lineOptions;

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);
                    if (point.get("lat") != null && point.get("lng") != null) {
                        double lat = Double.parseDouble(point.get("lat"));
                        double lng = Double.parseDouble(point.get("lng"));
                        LatLng position = new LatLng(lat, lng);

                        points.add(position);
                    }
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(8.0f);
                lineOptions.color(ViewCompat.MEASURED_STATE_MASK);
            }

            setMarkerOnMap(origin, dest);
            startAnim(points);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {
                origin = new LatLng(data.getDoubleExtra("originLat", 0.0),
                        data.getDoubleExtra("originLong", 0.0));
                Log.e("origin", "ActivityResult>><><>" + origin);

                dest = new LatLng(data.getDoubleExtra("destLat", 0.0),
                        data.getDoubleExtra("destLong", 0.0));
                Log.e("dest", "ActivityResult>><><>" + dest);

                if (mMap != null) {

                    mMap.clear();
                    showLoading();

                    String url = getDirectionsUrl(origin, dest);
                    DownloadTask downloadTask = new DownloadTask();
                    downloadTask.execute(url);
                }
            }
        }
    }

    public void showLoading() {
        hideLoading();
        mProgressDialog = showLoadingDialog(this);
    }

    public void hideLoading() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.cancel();
        }
    }

    public static ProgressDialog showLoadingDialog(Context context) {
        ProgressDialog progressDialog = null;
        try {
            progressDialog = new ProgressDialog(context);
            progressDialog.show();
            if (progressDialog.getWindow() != null) {
                progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            progressDialog.setContentView(R.layout.progress_dialog);
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return progressDialog;
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}