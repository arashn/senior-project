package com.mycompany.campusguide;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telecom.ConnectionRequest;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutionException;

public class MyActivity extends AppCompatActivity
        implements ConnectionCallbacks, OnConnectionFailedListener {
    private GoogleApiClient mGoogleApiClient;
    private BluetoothAdapter mBluetoothAdapter;
    public static final int REQUEST_ENABLE_BT = 2;

    // Create a BroadcastReceiver for ACTION_STATE_CHANGED
    // Turns Bluetooth on if it was turned off
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                if (state == BluetoothAdapter.STATE_OFF) {
                    System.out.println("Bluetooth was turned off. Turning on...");
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            System.out.println("Device does not support Bluetooth");
        }
        else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

        // Create an instance of GoogleAPIClient
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    public void onDestroy() {
        // Unregister BroadcastReceiver
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_my, menu);
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

    @Override
    public void onConnected(Bundle connectionHint) {

    }

    @Override
    public void onConnectionSuspended(int cause) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {

    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    /** Called when the user clicks the Request button */
    public void sendRequest(View view) {
        String location = ""; // String to store phone's coordinates
        String userData = "";
        // Check for permission to access device's fine location
        int permissionCheck = ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION");
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            // App has permission to access device's fine location
            // Get device's fine location and store it in location
            // location stores coordinates in the form "latitude, longitude"
            System.out.println("Permission granted");
            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
                String mLatitudeText = String.valueOf(mLastLocation.getLatitude());
                String mLongitudeText = String.valueOf(mLastLocation.getLongitude());
                location = mLatitudeText + ", " + mLongitudeText;
                System.out.println(location);
            }
        }
        else {
            System.out.println("Permission denied");
        }

        // Get the Bluetooth address for this device
        String btAddr = mBluetoothAdapter.getAddress();
        System.out.println(btAddr);
        userData = btAddr + ";" + location;

        // Create a worker thread to create a socket connected to the server
        // Get the socket's output stream, and write the phone's Blueooth address
        // and the current location, separated by a semicolon
        try {
            AsyncTask<Void, Void, Socket> socketConnection = new ServerConnection().execute();
            Socket sock = socketConnection.get();
            OutputStream socketOutput = sock.getOutputStream();
            socketOutput.write(userData.getBytes());
        }
        catch (IOException e) {
            System.err.println("An error occurred");
        }
        catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
        catch (ExecutionException e) {
            System.err.println(e.getMessage());
        }
    }
}
