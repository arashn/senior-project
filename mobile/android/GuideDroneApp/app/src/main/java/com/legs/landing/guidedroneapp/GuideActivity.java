package com.legs.landing.guidedroneapp;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class GuideActivity extends FragmentActivity {

    //UI elements
    private Button Btn_Land;
    private TextView Status;
    GoogleMap map;

    ArrayList<LatLng> markerPoints;
    MarkerOptions options1;
    LatLng UserlatLng; //user current lat,lng
    LatLng DestlatLng; //destination lat,lng

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        //Map UI
        SupportMapFragment fm = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map);
        map = fm.getMap();

        //Status box UI
        Status = (TextView) findViewById(R.id.statusInfo);
        Status.setText("No Status");



        //Disarm button UI
        Btn_Land = (Button) findViewById(R.id.btn_disarm);
        Btn_Land.setVisibility(View.INVISIBLE);

        //Bundle passed from MainActivity
        final Bundle destinationInfo = getIntent().getExtras();
        ArrayList<String> waypointLats = destinationInfo.getStringArrayList("path_lats");
        ArrayList<String> waypointLngs = destinationInfo.getStringArrayList("path_lngs");
        double ulat = destinationInfo.getDouble("ulat");
        double ulng = destinationInfo.getDouble("ulng");
        double dlat = destinationInfo.getDouble("dlat");
        double dlng = destinationInfo.getDouble("dlng");

        //Make connection to Server and send user;destination locations
        String user_dest_data = "nav;"+ ulat + "," + ulng + ";" + dlat + "," + dlng;
        ServerConn SendPathTask = new ServerConn();
        SendPathTask.execute(user_dest_data);
        Status.setText("Loading Path");

        //Land button listener
        Btn_Land.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                ServerConn LandTask = new ServerConn();

                LandTask.execute("land");
            }
        });


        //Adding waypoints to the map
        UserlatLng = new LatLng(ulat, ulng);
        DestlatLng = new LatLng(dlat, dlng);
        markerPoints = new ArrayList();

        options1 = new MarkerOptions();


        if(map!=null){


            map.setMyLocationEnabled(true);

            //add user and destination location markers
            options1.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            options1.position(UserlatLng);
            map.addMarker(options1);
            options1.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            options1.position(DestlatLng);
            map.addMarker(options1);

            //generate path between user and destination
            PolylineOptions lineOptions = new PolylineOptions();
            ArrayList<LatLng> points = new ArrayList<>();


            for (int j = 0; j < waypointLats.size(); j++) {

                double wlat = Double.valueOf(waypointLats.get(j));
                double wlng = Double.valueOf(waypointLngs.get(j));

                LatLng position = new LatLng(wlat, wlng);
                points.add(position);
            }

            //draw route
            lineOptions.addAll(points);
            lineOptions.width(8);
            lineOptions.color(Color.MAGENTA);


            //add path to map
            map.addPolyline(lineOptions);
            map.setBuildingsEnabled(true);
            //zoom into center of aldrich park
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(33.645918, -117.842730), 16.5F));
        }


    }

    /* A class to create a socket connected to the server in a worker thread */
    private class ServerConn extends AsyncTask<String, String, String> {
        protected Socket sock;
        protected OutputStream socketOutput;
        protected InputStream socketInput;

        protected String doInBackground(String... params) {
            String command = params[0];
            try {
                sock = new Socket("169.234.59.52", 9999); //YOUR SERVER'S IP ADDRESS
                socketOutput = sock.getOutputStream();
                socketOutput.write(command.getBytes());
                socketInput = sock.getInputStream();
                byte[] buffer = new byte[1024];
                socketInput.read(buffer);
                String input = new String(buffer);
                return input;
            } catch (UnknownHostException e) {
                System.err.println(e.getMessage());
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
            catch(Exception e){
                System.err.println(e.getMessage());
            }

            return "Socket Error";

        }
        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(String result){
            // Instantiating ParserTask which parses the json data from Geocoding webservice
            // in a non-ui thread
            System.out.println("SERVER ASYNC RESULT:" + result);

            if(result.contains("guideOK")){

                Btn_Land.setVisibility(View.VISIBLE);
                Status.setText("Guide is on the way!");

            }
            if(result.contains("landOK")){

                Status.setText("Guide has landed");
            }

            try{
            sock.close();
            }catch (Exception e){
                System.out.println("Close socket exception");
            }
        }


    }
}