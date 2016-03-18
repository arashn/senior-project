package com.legs.landing.guidedroneapp;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.android.PolyUtil;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsStep;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.TravelMode;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    //for calling GoogleMaps API
    GeoApiContext context_directions;
    GeoApiContext context_geocoding;
    //UI elements
    private Button Btn_Request;
    private EditText Edit_Loc;
    //for user current location
    private GoogleApiClient mGoogleApiClient;
    String mLatitudeText;
    String mLongitudeText;
    String userLocation;
    //for desired destination
    String desiredDestination;
    LatLng destLocation;
    String destString;
    double dlat;
    double dlng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // userLocation = "";
        desiredDestination = "";
        dlat = dlng = 0.0;
        destString = "";

        // Create an instance of GoogleAPIClient
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        setContentView(R.layout.activity_main);

        context_directions = new GeoApiContext().setApiKey("AIzaSyBj8RNUHUSuk78N"); //YOUR DIRECTIONS API KEY
        context_geocoding = new GeoApiContext().setApiKey("AIzaSyCGd6FdhXNqc"); //YOUR GEOCODE API KEY

        Edit_Loc = (EditText) findViewById(R.id.edit_desired);
        Edit_Loc.setBackgroundColor(Color.WHITE);

        Btn_Request = (Button) findViewById(R.id.btn_show);
        Btn_Request.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                //get user input from textbox
                desiredDestination = Edit_Loc.getText().toString();

                //convert address to lat,lng coordinates
                try {
                    GeocodingResult[] results = GeocodingApi.geocode(context_geocoding,
                            desiredDestination).await();
                    System.out.println(results[0].formattedAddress);
                    destLocation = new LatLng(results[0].geometry.location.lat, results[0].geometry.location.lng);
                }  catch (Exception e) {
                    System.out.println(e.getMessage());
                }


                    destString = destLocation.latitude + "," + destLocation.longitude;
                    dlat = destLocation.latitude;
                    dlng = destLocation.longitude;

                    //hardcoded desired location value
                   //  String x = desiredDestination.split(",")[0];
                  //   String y = desiredDestination.split(",")[1];
                  //  dlat = Double.valueOf(x.toString());
                  //  dlng = Double.valueOf(y.toString());
                 //  //destString = desiredDestination;

                //Get path between users and destination
                ArrayList<String> waypointLats = new ArrayList();
                ArrayList<String> waypointLngs = new ArrayList();
              //  System.out.println("destString: " + destString);


                //get path between user and destination and store values to be sent to next activity to display in map
                try {
                    DirectionsResult result = DirectionsApi.getDirections(context_directions, userLocation, destString).mode(TravelMode.WALKING).await();
                    for (DirectionsStep d : result.routes[0].legs[0].steps) {
                        for (LatLng point : PolyUtil.decode(d.polyline.getEncodedPath())) {
                            waypointLats.add(point.latitude + "");
                            waypointLngs.add(point.longitude + "");
                           // System.out.println("USER TO DEST: " + point.latitude + ", " + point.longitude);
                        }
                    }
                }
                catch (Exception e) {
                    System.out.println(e.getMessage());
                }



                //Start new activity
                Intent mapPage = new Intent(getApplicationContext(),GuideActivity.class);
                Bundle destinationInfo = new Bundle();

                destinationInfo.putStringArrayList("path_lats", waypointLats);
                destinationInfo.putStringArrayList("path_lngs", waypointLngs);
                destinationInfo.putDouble("dlat", dlat);
                destinationInfo.putDouble("dlng", dlng);
               destinationInfo.putDouble("ulat", Double.valueOf(mLatitudeText));
                destinationInfo.putDouble("ulng", Double.valueOf(mLongitudeText));
              //  destinationInfo.putDouble("ulat", 33.645890);
              //  destinationInfo.putDouble("ulng", -117.845049);
                mapPage.putExtras(destinationInfo);

                startActivity(mapPage);

            }

        });



    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            mLatitudeText= String.valueOf(mLastLocation.getLatitude());
            mLongitudeText = String.valueOf(mLastLocation.getLongitude());
            userLocation = mLatitudeText + "," + mLongitudeText;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }
}
