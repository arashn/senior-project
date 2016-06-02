package com.mycompany.campusguide;

import android.os.AsyncTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class GetDroneIPTask extends AsyncTask<String, Void, String> {
    protected Socket sock;
    protected String droneIPAddress = "";
    protected String doInBackground(String... location) {
        try {
            sock = new Socket("192.168.1.108", 9999);
            OutputStream socketOutput = sock.getOutputStream();
            socketOutput.write(location[0].getBytes());
            InputStream socketInput = sock.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(7);
            int nextByte = socketInput.read();
            while (nextByte != -1) {
                buffer.write(nextByte);
                nextByte = socketInput.read();
            }
            //byte[] response = new byte[15];
            //System.out.println("Bytes available: " + socketInput.read());
            //socketInput.read(response);
            droneIPAddress = buffer.toString();
            System.out.println("Drone IP: " + droneIPAddress);
        }
        catch (UnknownHostException e) {
            System.err.println(e.getMessage());
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
        }

        return droneIPAddress;
    }
}
