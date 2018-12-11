package dev.uberstyle.map;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class WhereToGo extends FragmentActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private String TAG = WhereToGo.class.getSimpleName();

    @BindView(R.id.edtPickUp)
    AutoCompleteTextView edtPickUp;

    @BindView(R.id.edtWhereTo)
    AutoCompleteTextView edtWhereTo;

    private Double originLat = 0.0;
    private Double originLong = 0.0;
    private Double destLat = 0.0;
    private Double destLong = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wheretogo);
        ButterKnife.bind(this);

        if (!NetworkUtils.isNetworkConnected(this)) {
            showMessage("Network Not Connected");
            return;
        }

        Boolean isPermission = MarshmallowPermissions.checkPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (!isPermission) {
            MarshmallowPermissions.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
            return;
        }

        setAdapterAutoComplete();
        setAdapterAutoComplete2();

        if (getIntent().getStringExtra("address") != null && !getIntent().getStringExtra("address").equals("")) {

            originLat = getIntent().getDoubleExtra("latitude", 0.0);
            originLong = getIntent().getDoubleExtra("longitude", 0.0);

            Log.e("origin", "originLatLongUp>>><>" + originLat + " <><<>" + originLong);
            edtPickUp.setText(getIntent().getStringExtra("address"));
            edtWhereTo.requestFocus();
        } else {
            edtPickUp.requestFocus();
        }
    }

    private void setAdapterAutoComplete() {
        edtPickUp.setAdapter(new GooglePlacesAutocompleteAdapter(this, R.layout.list_item_search_autocomplete));
        edtPickUp.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.e("Origin", "Origin>>" + parent.getItemAtPosition(position).toString());
                getLocationFromAddress(parent.getItemAtPosition(position).toString(), true);

                if (edtWhereTo.getText().toString().equals("")) {
                    edtWhereTo.requestFocus();
                    showMessage("Please Enter Destination Location");
                    return;
                }

                KeyboardUtils.hideSoftInput(WhereToGo.this);
                Intent output = new Intent();
                output.putExtra("originLat", originLat);
                output.putExtra("originLong", originLong);
                output.putExtra("destLat", destLat);
                output.putExtra("destLong", destLong);
                setResult(RESULT_OK, output);
                finish();
            }
        });
    }

    private void setAdapterAutoComplete2() {
        edtWhereTo.setAdapter(new GooglePlacesAutocompleteAdapter(this, R.layout.list_item_search_autocomplete));
        edtWhereTo.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.e("Destination", "Destination>>" + parent.getItemAtPosition(position).toString());
                getLocationFromAddress(parent.getItemAtPosition(position).toString(), false);
                if (edtPickUp.getText().toString().equals("")) {
                    edtPickUp.requestFocus();
                    showMessage("Please Enter Pickup Location");
                    return;
                }

                KeyboardUtils.hideSoftInput(WhereToGo.this);
                Intent output = new Intent();
                output.putExtra("originLat", originLat);
                output.putExtra("originLong", originLong);
                output.putExtra("destLat", destLat);
                output.putExtra("destLong", destLong);
                setResult(RESULT_OK, output);
                finish();
            }
        });
    }

    @OnClick(R.id.btnBack)
    void onBackClick() {
        finish();
    }

    public void getLocationFromAddress(String strAddress, Boolean isOrigin) {
        Geocoder coder = new Geocoder(this);
        List<Address> address;

        try {
            address = coder.getFromLocationName(strAddress, 5);
            if (address == null) {
                return;
            }
            Address location = address.get(0);

            if (isOrigin) {
                originLat = location.getLatitude();
                originLong = location.getLongitude();
                Log.e("origin", "originLatLong>>><>" + originLat + "<><><><>" + originLong);
            } else {
                destLat = location.getLatitude();
                destLong = location.getLongitude();
                Log.e("dest", "destLatLong>>><>" + destLat + "<><><><>" + destLong);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 100: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setAdapterAutoComplete();
                    setAdapterAutoComplete2();

                    if (getIntent().getStringExtra("address") != null && !getIntent().getStringExtra("address").equals("")) {

                        originLat = getIntent().getDoubleExtra("latitude", 0.0);
                        originLong = getIntent().getDoubleExtra("longitude", 0.0);

                        Log.e("origin", "originLatLongUp>>><>" + originLat + " <><<>" + originLong);
                        edtPickUp.setText(getIntent().getStringExtra("address"));
                        edtWhereTo.requestFocus();
                    } else {
                        edtPickUp.requestFocus();
                    }
                } else {
                    Log.e(TAG, "Permission Not Granted");
                    showMessage(getString(R.string.allow_storage_permission));
                }

                break;
            }
        }
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}