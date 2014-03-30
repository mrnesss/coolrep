package com.worldcanvas.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.MapFragment;

/**
 * Created by Mario on 3/23/2014.
 */
public class CameraActivity extends FragmentActivity implements GooglePlayServicesClient.ConnectionCallbacks,
                                                                GooglePlayServicesClient.OnConnectionFailedListener,
                                                                LocationListener {
    private Camera camera;
    private CameraPreview cameraPreview;
    private GLSurfaceView glView;
    private LocationClient locationClient;
    private Location currentLocation;
    private LocationRequest requestLocation;
    private boolean requestUpdates;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;



    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private final static int MILLI_SECONDS_REQUEST = 2500;
    private final static int MAX_MILLI_SECONDS_REQUEST = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_preview);
        camera = getCameraInstance();
        camera.setDisplayOrientation(90);
        cameraPreview = new CameraPreview(this, camera);

        //create the locationClient to get the actual location
        locationClient = new LocationClient(this,this,this);


        //request each location with some speed request parameters and acuracy
        requestLocation = LocationRequest.create();
        requestLocation.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        requestLocation.setInterval(MILLI_SECONDS_REQUEST);
        requestLocation.setFastestInterval(MILLI_SECONDS_REQUEST);


        preferences = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
        editor = preferences.edit();
        requestUpdates = false;


        glView = new GLSurfaceView(this);
        glView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        glView.setRenderer(new GLRenderer());

        FrameLayout preview = (FrameLayout)this.findViewById(R.id.camera_preview);
        preview.addView(glView);
        preview.addView(cameraPreview);
    }

    @Override
    protected void onPause() {
        editor.putBoolean("KEY_UPDATES_ON",requestUpdates);
        editor.commit();
        super.onPause();
        releaseCamera();
        glView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        glView.onResume();
        if (preferences.contains("KEY_UPDATES_ON")){
            requestUpdates = preferences.getBoolean("KEY_UPDATES_ON", true);
        }else{
            editor.putBoolean("KEY_UPDATES_ON", false);
            editor.commit();
        }
    }



    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private void releaseCamera() {
        if(camera != null) {
            camera.release();
            camera = null;
        }
    }

    /* estos metodos los traen las funciones callbacks de  GooglePlayServicesClient.ConnectionCallbacks,
                                                           GooglePlayServicesClient.OnConnectionFailedListener*/
    @Override
    public void onConnected(Bundle bundle) {
        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        if (locationClient.isConnected()){
           currentLocation = locationClient.getLastLocation();
           Log.e("current location",currentLocation.toString());
        }
        //if (requestUpdates) {
            locationClient.requestLocationUpdates(requestLocation, this);
        //}

    }

    @Override
    public void onDisconnected() {
        Toast.makeText(this, "Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()){
            try {
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e){
                e.printStackTrace();
            }
        } else{
            showErrorDialog(connectionResult.getErrorCode());
        }
    }
    /*terminan las funciones callbacks */
    //este metodo despliega un mensaje de error cuando no se pueden resolver problemas
    private void showErrorDialog(int errorCode) {
        Toast.makeText(this,errorCode+"",Toast.LENGTH_SHORT).show();
    }

    /*estos metodos manejan si a parado o no la actividad de gps */
        /*
     * Called when the Activity becomes visible.
     */
    @Override
    protected void onStart() {
        super.onStart();
        // Connect the client.
        locationClient.connect();
    }

    /*
     * Called when the Activity is no longer visible.
     */
    @Override
    protected void onStop() {
        // Disconnecting the client invalidates it.
        if (locationClient.isConnected()){
            locationClient.removeLocationUpdates(this);
        }
        locationClient.disconnect();
        super.onStop();
    }

    /*detectar si ya se connecto para obtener el current location */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case CONNECTION_FAILURE_RESOLUTION_REQUEST:
                switch (resultCode){
                    case Activity.RESULT_OK:

                        break;
                }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location.getAccuracy() < 90)
            Log.e("la latitud "+location.getLatitude()+" ",location.getLongitude()+" ");
    }

    /*retreive new locations */



}
