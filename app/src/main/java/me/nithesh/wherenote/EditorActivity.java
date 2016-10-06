package me.nithesh.wherenote;

/**
 * Created by Nithesh on 8/26/2016.
 */

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.List;
import java.util.Locale;


public class EditorActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private String action;
    private EditText editor;
    private EditText addressText;
    private TextView latid;
    private TextView lonid;
    private String noteFilter;
    private String oldText;
    private String oldAddress;
    private String mapsLink;
    private String latitude;
    private String longitude;
    private String displayed_latitude;
    private String displayed_longitude;
    private boolean clicked;
    private List<Address> addressList;
    private String pAddress;
    protected static final String TAG = "Location Log";
    private LocationManager mLocationManager;
    private Location mLocation;
    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    private static final int REQUEST_LOCATION = 0;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        editor = (EditText) findViewById(R.id.editText);
        latid = (TextView) findViewById(R.id.lat_id);
        lonid = (TextView) findViewById(R.id.lon_id);
        addressText = (EditText) findViewById(R.id.addressText);
        clicked = false;
        Intent intent = getIntent();
        Uri uri = intent.getParcelableExtra(NotesProvider.CONTENT_ITEM_TYPE);

        if (uri == null) {
            action = Intent.ACTION_INSERT;
            setTitle(getString(R.string.new_note));
        } else {
            action = Intent.ACTION_EDIT;
            noteFilter = DBOpenHelper.NOTE_ID + "=" + uri.getLastPathSegment();

            Cursor cursor = getContentResolver().query(uri,
                    DBOpenHelper.ALL_COLUMNS, noteFilter, null, null);
            cursor.moveToFirst();


            oldText = cursor.getString(cursor.getColumnIndex(DBOpenHelper.NOTE_TEXT));
            editor.setText(oldText);
            oldAddress = cursor.getString(cursor.getColumnIndex(DBOpenHelper.ADDRESS));
            addressText.setText(oldAddress);
            displayed_longitude = cursor.getString(cursor.getColumnIndex(DBOpenHelper.LON));
            displayed_latitude = cursor.getString(cursor.getColumnIndex(DBOpenHelper.LAT));
            editor.requestFocus();
            latid.setText(displayed_latitude);
            lonid.setText(displayed_longitude);
            mapsLink = cursor.getString(cursor.getColumnIndex(DBOpenHelper.MAP_LINK));

        }
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (action.equals(Intent.ACTION_EDIT)) {
            getMenuInflater().inflate(R.menu.menu_editor, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                finishEditing();
                break;
            case R.id.action_delete:
                deleteNote();
                break;
        }

        return true;
    }

    private void deleteNote() {
        getContentResolver().delete(NotesProvider.CONTENT_URI,
                noteFilter, null);
        Toast.makeText(this, getString(R.string.note_deleted),
                Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
        finish();
    }

    private void finishEditing() {
        String newText = editor.getText().toString().trim();
        String newAddress = addressText.getText().toString().trim();

        switch (action) {
            case Intent.ACTION_INSERT:
                if (newText.length() == 0) {
                    setResult(RESULT_CANCELED);
                } else {
                    insertNote(newText);
                }
                break;
            case Intent.ACTION_EDIT:
                if (newText.length() == 0)
                {
                    deleteNote();
                }

                else if(clicked || oldText != newText || oldAddress != newAddress)
                {
                    updateNote(newText,newAddress);
                }

                else
                {
                    setResult(RESULT_CANCELED);
                }

        }

        finish();
    }

    private void updateNote(String noteText, String addressTxt) {
        ContentValues values = new ContentValues();
        values.put(DBOpenHelper.NOTE_TEXT, noteText);
        values.put(DBOpenHelper.MAP_LINK, mapsLink);
        values.put(DBOpenHelper.LAT, displayed_latitude);
        values.put(DBOpenHelper.LON, displayed_longitude);
        values.put(DBOpenHelper.ADDRESS, addressTxt);
        getContentResolver().update(NotesProvider.CONTENT_URI, values, noteFilter, null);
        Toast.makeText(this, getString(R.string.note_updated), Toast.LENGTH_SHORT).show();
        setResult(RESULT_OK);
    }

    private void insertNote(String noteText) {
        ContentValues values = new ContentValues();
        values.put(DBOpenHelper.NOTE_TEXT, noteText);
        values.put(DBOpenHelper.MAP_LINK, mapsLink);
        values.put(DBOpenHelper.LAT, displayed_latitude);
        values.put(DBOpenHelper.LON, displayed_longitude);
        values.put(DBOpenHelper.ADDRESS, addressText.getText().toString().trim());
        getContentResolver().insert(NotesProvider.CONTENT_URI, values);
        setResult(RESULT_OK);
    }

    public void getLocForNewNote(View view) {
            clicked = true;
            addressText.setText("");
            Toast.makeText(this, getString(R.string.getting_), Toast.LENGTH_SHORT).show();
            for(int i = 0; i < 8; i++) {
                latid.setText(latitude);
                lonid.setText(longitude);
                displayed_longitude = longitude;
                displayed_latitude = latitude;
                String label = editor.getText().toString().trim().replaceAll(" ", "+");
                mapsLink = String.format("geo:0,0?q=%s,%s(%s)", displayed_latitude, displayed_longitude, label);
                pAddress = getAddress();
                addressText.setText(pAddress);
            }
            if(displayed_latitude == null && displayed_longitude == null)
            {
                Toast.makeText(this, getString(R.string.click_settings), Toast.LENGTH_SHORT).show();
            }


    }

    private String getAddress()
    {
        String result = "";

                if (addressList != null && addressList.size() > 0) {
                    Address address = addressList.get(0);
                    StringBuilder sb = new StringBuilder();
                    for(int i = 0; i < address.getMaxAddressLineIndex(); i++)
                    {
                        sb.append(address.getAddressLine(i)).append("\n");
                    }
                    sb.append(address.getCountryCode());
                    result = sb.toString();
                    result = result + " (approx.)";
                    Log.i(TAG, result);
                }

        return result;
    }

    public void goToGmap(View view) {
       // startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(mapsLink)));
        Uri gmmIntentUri = Uri.parse(mapsLink);
        Log.i(TAG, mapsLink);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }


    @Override
    public void onBackPressed() {
        finishEditing();
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Editor Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://me.nithesh.wherenote/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Editor Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://me.nithesh.wherenote/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);

        client.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.perm_err), Toast.LENGTH_SHORT).show();
            // Check Permissions Now
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION);
        } else {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    // Request Permissions
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            if(grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // We can now safely use the API we requested access to
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            } else {
                Log.i(TAG, "Permission was denied or request was cancelled");
                Toast.makeText(this, getString(R.string.gps_err), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged() called");
        Log.i(TAG, "Location: " + location.toString());
        latitude = String.valueOf(location.getLatitude()) ;
        longitude = String.valueOf(location.getLongitude());
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "GoogleApiClient connection failed");
    }
}
