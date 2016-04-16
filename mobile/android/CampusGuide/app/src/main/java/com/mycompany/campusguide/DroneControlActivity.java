package com.mycompany.campusguide;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
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
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.mission.item.command.Takeoff;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Land;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Waypoint;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

import java.util.ArrayList;
import java.util.List;


public class DroneControlActivity extends AppCompatActivity
        implements DroneListener, TowerListener, ConnectionCallbacks, OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private GeoApiContext context;

    private ControlTower controlTower;

    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private final Handler handler = new Handler();
    Spinner modeSelector;

    private VehicleApi vehicleApi;
    private MissionApi missionApi;
    private SimpleCommandListener commandListener;

    private ArrayList<String> directions;
    private Mission mission;

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
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                // Prepare the drone for its mission
                prepareDrone();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                break;

            case AttributeEvent.STATE_UPDATED:
            case AttributeEvent.STATE_ARMING:
                updateArmButton();
                updateConnectedButton(this.drone.isConnected());
                updateStartButton();
                updateButtons();
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                updateVehicleMode();
                break;

            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != this.droneType) {
                    this.droneType = newDroneType.getDroneType();
                    updateVehicleModesForType(this.droneType);
                }
                break;

            case AttributeEvent.SPEED_UPDATED:
                updateAltitude();
                updateSpeed();
                break;

            case AttributeEvent.HOME_UPDATED:
                updateDistanceFromHome();
                break;

            case AttributeEvent.MISSION_SENT:
                alertUser("Mission sent");
                break;

            case AttributeEvent.MISSION_RECEIVED:
                System.out.println("Mission received");
                break;

            case AttributeEvent.MISSION_UPDATED:
                System.out.println("Mission updated");
                break;

            default:
                //Log.i("DRONE_EVENT", event);
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
        setContentView(R.layout.activity_drone_control);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize the service manager
        this.controlTower = new ControlTower(getApplicationContext());

        this.drone = new Drone(getApplicationContext());

        this.modeSelector = (Spinner) findViewById(R.id.modeSelect);
        this.modeSelector.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFlightModeSelected(view);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        this.commandListener = new SimpleCommandListener();

        // Create an instance of GoogleAPIClient
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        context = new GeoApiContext().setApiKey("AIzaSyBXrspoL1Oy5QhAZg5zWOjoTSkQZ-itW7g");
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

    @Override
    public void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
        this.controlTower.connect(this);
    }

    @Override
    public void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            updateConnectedButton(false);
        }
        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    protected void updateConnectedButton(Boolean isConnected) {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Button connectButton = (Button) findViewById(R.id.btnConnect);
        if (isConnected) {
            connectButton.setText("Disconnect");
        } else {
            connectButton.setText("Connect");
        }

        // Disconnect not allowed while drone is armed
        if (vehicleState.isConnected() && vehicleState.isArmed()) {
            connectButton.setEnabled(false);
        } else {
            connectButton.setEnabled(true);
        }
    }

    protected void updateArmButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Button armButton = (Button) findViewById(R.id.btnArm);

        if (this.drone.isConnected()) {
            armButton.setEnabled(true);
        } else {
            armButton.setEnabled(false);
        }

        if (vehicleState.isArmed()) {
            // Connected and Armed
            armButton.setText("DISARM");
        } else if (vehicleState.isConnected()) {
            // Connected but not Armed
            armButton.setText("ARM");
        }
    }

    protected void updateStartButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Button startButton = (Button) findViewById(R.id.btnStartMission);

        if (vehicleState.isArmed() && !vehicleState.isFlying()) {
            startButton.setEnabled(true);
        } else {
            startButton.setEnabled(false);
        }
    }

    protected void updateButtons() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();

        Button landButton = (Button) findViewById(R.id.btnLand);
        Button returnToLaunchButton = (Button) findViewById(R.id.btnRTL);

        if (vehicleMode == VehicleMode.COPTER_AUTO && vehicleState.isFlying()) {
            landButton.setEnabled(true);
            returnToLaunchButton.setEnabled(true);
        } else {
            landButton.setEnabled(false);
            returnToLaunchButton.setEnabled(false);
        }
    }

    public void onBtnConnectTap(View view) {
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            vehicleApi = null;
            missionApi = null;
        } else {
            ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(14550);
            this.drone.connect(connectionParams);
            vehicleApi = VehicleApi.getApi(this.drone);
            missionApi = MissionApi.getApi(this.drone);
        }
    }

    public void onArmButtonTap(View view) {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleState.isConnected() && !vehicleState.isArmed()) {
            // Connected but not Armed
            vehicleApi.arm(true);
            alertUser("Vehicle armed");
        } else {
            vehicleApi.arm(false);
            alertUser("Vehicle disarmed");
        }
    }

    public void onStartButtonTap(View view) {
        /*State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleState.isFlying()) {
            // Land
            vehicleApi.setVehicleMode(VehicleMode.COPTER_LAND);
            alertUser("Landing");
        } else if (vehicleState.isArmed()) {
            // Take off
            controlApi.takeoff(10, commandListener); // Default take off altitude is 10m
            alertUser("Taking off");
        }*/

        //startMission();
        missionApi.startMission(true, false, commandListener);
    }

    public void onLandButtonTap(View view) {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleState.isFlying()) {
            // Land
            vehicleApi.setVehicleMode(VehicleMode.COPTER_LAND);
            alertUser("Landing");
        }
    }

    public void onReturnToLaunchButtonTap(View view) {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleState.isFlying()) {
            // Return to Launch
            vehicleApi.setVehicleMode(VehicleMode.COPTER_RTL);
        }
    }

    public void onFlightModeSelected(View view) {
        VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();
        vehicleApi.setVehicleMode(vehicleMode);
    }

    protected void updateVehicleModesForType(int droneType) {
        List<VehicleMode> vehicleModes = VehicleMode.getVehicleModePerDroneType(droneType);
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this, android.R.layout.simple_spinner_item, vehicleModes);
        vehicleModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.modeSelector.setAdapter(vehicleModeArrayAdapter);
    }

    protected void updateVehicleMode() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        ArrayAdapter arrayAdapter = (ArrayAdapter) this.modeSelector.getAdapter();
        this.modeSelector.setSelection(arrayAdapter.getPosition(vehicleMode));
    }

    protected void updateAltitude() {
        TextView altitudeTextView = (TextView) findViewById(R.id.altitudeValueTextView);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        altitudeTextView.setText(String.format("%3.1f", droneAltitude.getAltitude()) + "m");
    }

    protected void updateSpeed() {
        TextView speedTextView = (TextView) findViewById(R.id.speedValueTextView);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        speedTextView.setText(String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");
    }

    protected void updateDistanceFromHome() {
        TextView distanceTextView = (TextView) findViewById(R.id.distanceValueTextView);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        double vehicleAltitude = droneAltitude.getAltitude();
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        LatLong vehiclePosition = droneGps.getPosition();

        double distanceFromHome = 0;

        if (droneGps.isValid()) {
            LatLongAlt vehicle3DPosition = new LatLongAlt(vehiclePosition.getLatitude(), vehiclePosition.getLongitude(), vehicleAltitude);
            Home droneHome = this.drone.getAttribute(AttributeType.HOME);
            distanceFromHome = distanceBetweenPoints(droneHome.getCoordinate(), vehicle3DPosition);
        } else {
            distanceFromHome = 0;
        }

        distanceTextView.setText(String.format("%3.1f", distanceFromHome) + "m");
    }

    protected double distanceBetweenPoints(LatLongAlt pointA, LatLongAlt pointB) {
        if (pointA == null || pointB == null) {
            return 0;
        }
        double dx = pointA.getLatitude() - pointB.getLatitude();
        double dy = pointA.getLongitude() - pointB.getLongitude();
        double dz = pointA.getAltitude() - pointB.getAltitude();
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    public String getDeviceLocation() {
        String location = "";
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
            }
        }
        else {
            System.out.println("Permission denied");
        }

        return location;
    }

    public LatLong getPosition() {
        Gps droneGps = drone.getAttribute(AttributeType.GPS);
        return isValid() ? droneGps.getPosition() : null;
    }

    public boolean isValid() {
        Gps droneGps = drone.getAttribute(AttributeType.GPS);
        return droneGps != null && droneGps.isValid();
    }

    public ArrayList<String> getDirections(String startLocation, String endLocation) {
        directions = new ArrayList<String>();
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
        /*for (String x : directions) {
            System.out.println(x);
        }*/
        return directions;
    }

    public void createMission() {
        System.out.println("Creating mission");
        //missionApi.loadWaypoints();
        // Get the mission property from the drone
        //mission = this.drone.getAttribute(AttributeType.MISSION);
        mission = new Mission();

        // Clear the mission first to remove old waypoints
        //mission.clear();

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
        land.setCoordinate(new LatLongAlt(0.0, 0.0, 0.0));
        mission.addMissionItem(land);

        // Upload the mission to the drone
        missionApi.setMission(mission, true);
        System.out.println("Done");
        //Mission received = this.drone.getAttribute(AttributeType.MISSION);
        //missionApi.loadWaypoints();
        //System.out.println(received.getMissionItems().get(0).toString());
    }

    public void prepareDrone() {
        // Get location of device
        String deviceLocation = getDeviceLocation();

        // Get position of drone
        LatLong dronePosition = getPosition();
        String droneLocation = dronePosition.getLatitude() + ", " + dronePosition.getLongitude();

        getDirections(droneLocation, "Ayala Science Library, Irvine, CA");

        createMission();
    }
}
