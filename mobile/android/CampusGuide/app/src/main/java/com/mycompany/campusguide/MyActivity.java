package com.mycompany.campusguide;

import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telecom.ConnectionRequest;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.android.PolyUtil;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.TravelMode;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.MissionApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.mission.item.command.Takeoff;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Land;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Waypoint;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Type;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class MyActivity extends AppCompatActivity
        implements ConnectionCallbacks, OnConnectionFailedListener, DroneListener, TowerListener {
    private GoogleApiClient mGoogleApiClient;
    private GeoApiContext context;

    private ControlTower controlTower;
    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private final Handler handler = new Handler();

    // 3DR Services Listener
    @Override
    public void onTowerConnected() {
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
    }

    @Override
    public void onTowerDisconnected() {

    }

    @Override
    public void onDroneEvent(String event, Bundle extras) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                break;

            default:
                break;
        }
    }

    @Override
    public void onDroneConnectionFailed(com.o3dr.services.android.lib.drone.connection.ConnectionResult result) {

    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

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

        // Create an instance of GoogleAPIClient
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        context = new GeoApiContext().setApiKey("AIzaSyBj8RNUHUSuk78N2Jim9yrMAKjWvh6gc_g");

        // Initialize the service manager
        this.controlTower = new ControlTower(getApplicationContext());
        this.drone = new Drone(getApplicationContext());
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
        this.controlTower.connect(this);

        Bundle extraParams = new Bundle();
        extraParams.putInt(ConnectionType.EXTRA_UDP_SERVER_PORT, 14550); // Set default port to 14550

        ConnectionParameter connectionParams = new ConnectionParameter(ConnectionType.TYPE_UDP, extraParams);
        this.drone.connect(connectionParams);
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
        }
        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

    /** Called when the user clicks the Request button */
    public void sendRequest(View view) {
        while (!this.drone.isConnected()) {
            Bundle extraParams = new Bundle();
            extraParams.putInt(ConnectionType.EXTRA_UDP_SERVER_PORT, 14550); // Set default port to 14550

            ConnectionParameter connectionParams = new ConnectionParameter(ConnectionType.TYPE_UDP, extraParams);
            this.drone.connect(connectionParams);
        }
        String location = ""; // String to store phone's coordinates
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
            LatLong dronePosition = getPosition();
            while (dronePosition == null) {
                dronePosition = getPosition();
            }
            String droneLocation = dronePosition.getLatitude() + ", " + dronePosition.getLongitude();
            System.out.println("Drone position: " + droneLocation);
            ArrayList<String> directions = getDirections("Aldrich Hall, Irvine, CA", "Ayala Science Library, Irvine, CA");
            createMission(directions);
        }
        else {
            System.out.println("Permission denied");
        }
    }

    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    public LatLong getPosition() {
        Gps droneGps = drone.getAttribute(AttributeType.GPS);
        while (!isValid()) {
            droneGps = drone.getAttribute(AttributeType.GPS);
        }
        return isValid() ? droneGps.getPosition() : null;
    }

    public boolean isValid() {
        Gps droneGps = drone.getAttribute(AttributeType.GPS);
        return droneGps != null && droneGps.isValid();
    }

    public ArrayList<String> getDirections(String startLocation, String endLocation) {
        ArrayList<String> directions = new ArrayList<String>();
        try {
            DirectionsResult directionsResult = DirectionsApi.getDirections(context, startLocation,
                    endLocation).mode(TravelMode.WALKING).await();
            com.google.maps.model.LatLng start = directionsResult.routes[0].legs[0].steps[0]
                    .startLocation;
            directions.add(start.lat + ", " + start.lng);
            for (DirectionsStep step : directionsResult.routes[0].legs[0].steps) {
                for (LatLng point : PolyUtil.decode(step.polyline.getEncodedPath())) {
                    directions.add(point.latitude + ", " + point.longitude);
                }
                com.google.maps.model.LatLng end = step.endLocation;
                directions.add(end.lat + ", " + end.lng);
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
        for (String x : directions) {
            System.out.println(x);
        }
        return directions;
    }

    public void createMission(ArrayList<String> directions) {
        System.out.println("Creating mission");
        // Get the mission property from the drone
        Mission mission = this.drone.getAttribute(AttributeType.MISSION);

        // Clear the mission first to remove old waypoints
        mission.clear();

        // Add Takeoff object with altitude of 5m
        Takeoff takeoff = new Takeoff();
        takeoff.setTakeoffAltitude(5.0);
        mission.addMissionItem(takeoff);

        // Add each coordinate in directions as a Waypoint object with altitude of 5m
        for (String point : directions) {
            double lat = Double.parseDouble(point.split(", ")[0]);
            double lon = Double.parseDouble(point.split(", ")[1]);

            LatLongAlt coordinate = new LatLongAlt(lat, lon, 5.0);
            Waypoint waypoint = new Waypoint();
            waypoint.setCoordinate(coordinate);
            mission.addMissionItem(waypoint);
        }

        // Add Land object
        Land land = new Land();
        mission.addMissionItem(land);

        // Upload the mission to the drone
        MissionApi missionApi = MissionApi.getApi(this.drone);
        missionApi.setMission(mission, true);
        System.out.println("Done");
        Mission received = this.drone.getAttribute(AttributeType.MISSION);
        missionApi.loadWaypoints();
        System.out.println(received.getMissionItems().get(0).toString());
    }
}
