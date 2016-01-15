package com.mycompany.campusguide;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

/* A class to create a socket connected to the server in a worker thread */
public class ServerConnection extends AsyncTask<Void, Void, Socket> {
    protected Socket sock;
    protected Socket doInBackground(Void... params) {
        try {
            sock = new Socket("169.234.11.144", 9999);
        }
        catch (UnknownHostException e) {
            System.err.println(e.getMessage());
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return sock;
    }
}
