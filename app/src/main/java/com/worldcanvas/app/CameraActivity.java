package com.worldcanvas.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Queue;

/**
 * Created by Mario on 3/23/2014.
 */
public class CameraActivity extends FragmentActivity implements GooglePlayServicesClient.ConnectionCallbacks,
                                                                GooglePlayServicesClient.OnConnectionFailedListener,
                                                                LocationListener,
                                                                SensorEventListener{
    private Camera camera;
    private CameraPreview cameraPreview;
    private GLSurfaceView glView;
    private LocationClient locationClient;
    private Location currentLocation;
    private LocationRequest requestLocation;
    private boolean requestUpdates;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    private JsonHttpResponseHandler handler = new JsonHttpResponseHandler(){
        @Override
        public void onFailure(Throwable arg0) {
            Toast.makeText(getApplicationContext(), "Network error, please try again later.",Toast.LENGTH_LONG).show();
        }
        @Override
        public void onSuccess(JSONArray ok) {
            succesSaveComment(ok);
        }
    };

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private final static int MILLI_SECONDS_REQUEST = 2500;
    private final static int MAX_MILLI_SECONDS_REQUEST = 1000;

    /* sensor data */
    SensorManager m_sensorManager;
    float []m_lastMagFields;
    float []m_lastAccels;
    private float[] m_rotationMatrix = new float[16];
    private float[] m_remappedR = new float[16];
    private float[] m_orientation = new float[4];

    /* fix random noise by averaging tilt values */
    final static int AVERAGE_BUFFER = 30;
    float []m_prevPitch = new float[AVERAGE_BUFFER];
    float m_lastPitch = 0.f;
    float m_lastYaw = 0.f;
    /* current index int m_prevEasts */
    int m_pitchIndex = 0;

    float []m_prevRoll = new float[AVERAGE_BUFFER];
    float m_lastRoll = 0.f;
    /* current index into m_prevTilts */
    int m_rollIndex = 0;

    /* center of the rotation */
    private float m_tiltCentreX = 0.f;
    private float m_tiltCentreY = 0.f;
    private float m_tiltCentreZ = 0.f;

    /* barometro */
    Sensor pressure;
    float altitude;
    Queue<Float> history;
    int historySize;
    float avgPressure;
    float ctemp;
    JSONObject json;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_preview);
        m_sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        registerListeners();
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
        //preview.addView(glView);
        preview.addView(cameraPreview);

        final FrameLayout shade = (FrameLayout)findViewById(R.id.shade);
        final Animation animationFadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        shade.startAnimation(animationFadeOut);

        //animation2 AnimationListener
        animationFadeOut.setAnimationListener(new Animation.AnimationListener(){

            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
            @Override
            public void onAnimationEnd(Animation arg0) {
                // start animation1 when animation2 ends (repeat)
                shade.setAlpha(0);
            }
            @Override
            public void onAnimationStart(Animation arg0) {}
            @Override
            public void onAnimationRepeat(Animation arg0) {}
        });

    }

    @Override
    protected void onPause() {
        editor.putBoolean("KEY_UPDATES_ON",requestUpdates);
        editor.commit();
        m_sensorManager.unregisterListener(this);
        releaseCamera();
        unregisterListeners();
        glView.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        m_sensorManager.registerListener(this, pressure, SensorManager.SENSOR_DELAY_GAME);
        registerListeners();
        glView.onResume();
        if (preferences.contains("KEY_UPDATES_ON")){
            requestUpdates = preferences.getBoolean("KEY_UPDATES_ON", true);
        }else{
            editor.putBoolean("KEY_UPDATES_ON", false);
            editor.commit();
        }
    }


    @Override
    public void onDestroy() {
        unregisterListeners();
        super.onDestroy();
    }

    private void registerListeners() {
        m_sensorManager.registerListener(this, m_sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
        m_sensorManager.registerListener(this, m_sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
    }

    private void unregisterListeners() {
        m_sensorManager.unregisterListener(this);
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
    /*retreive new locations */
    @Override
    public void onLocationChanged(Location location) {
        if (location.getAccuracy() < 90){
            currentLocation = location;
            Log.e("la latitud "+location.getLatitude()+" ",location.getLongitude()+" ");
        }
    }

    /*display toast message of succes*/
    public void succesSaveComment(JSONArray ok){
        try{
           Toast.makeText(this,ok.getJSONObject(0).getString("msg"),Toast.LENGTH_SHORT).show();
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
    private void accel(SensorEvent event) {
        if (m_lastAccels == null) {
            m_lastAccels = new float[3];
        }

        System.arraycopy(event.values, 0, m_lastAccels, 0, 3);

        /*if (m_lastMagFields != null) {
            computeOrientation();
        }*/
    }

    private void mag(SensorEvent event) {
        if (m_lastMagFields == null) {
            m_lastMagFields = new float[3];
        }

        System.arraycopy(event.values, 0, m_lastMagFields, 0, 3);

        if (m_lastAccels != null) {
            computeOrientation();
        }
    }

    Filter [] m_filters = { new Filter(), new Filter(), new Filter() };

    private class Filter {
        static final int AVERAGE_BUFFER = 10;
        float []m_arr = new float[AVERAGE_BUFFER];
        int m_idx = 0;

        public float append(float val) {
            m_arr[m_idx] = val;
            m_idx++;
            if (m_idx == AVERAGE_BUFFER)
                m_idx = 0;
            return avg();
        }
        public float avg() {
            float sum = 0;
            for (float x: m_arr)
                sum += x;
            return sum / AVERAGE_BUFFER;
        }

    }

    private void computeOrientation() {
        if (SensorManager.getRotationMatrix(m_rotationMatrix, null, m_lastAccels,m_lastMagFields)) {
            SensorManager.getOrientation(m_rotationMatrix, m_orientation);

            /* 1 radian = 57.2957795 degrees */
            /* [0] : yaw, rotation around z axis
             * [1] : pitch, rotation around x axis
             * [2] : roll, rotation around y axis */
            float yaw = m_orientation[0] * 57.2957795f;
            float pitch = m_orientation[1] * 57.2957795f;
            float roll = m_orientation[2] * 57.2957795f;

            m_lastYaw = m_filters[0].append(yaw);
            m_lastPitch = m_filters[1].append(pitch);
            m_lastRoll = m_filters[2].append(roll);
        }
    }

    /*save the comment*/
    public void saveComment(String msg){
        ConexionLayer.saveComment(msg,null
                ,m_lastPitch,m_lastRoll,m_lastYaw
                ,currentLocation.getLongitude(),currentLocation.getLatitude(),altitude,handler);
    }

    private class JSONWeatherTask extends AsyncTask<String, Void, JSONObject> {

        String url = "http://api.openweathermap.org/data/2.5/weather?q=";
        JSONObject json;

        //http://openweathermap.org/data/2.3/forecast/city?id=524901&APPID=1111111111

        public String getWeatherData(String location) {
            HttpURLConnection con = null ;
            InputStream is = null;

            try {
                con = (HttpURLConnection) (new URL(url + location)).openConnection();
                con.setRequestMethod("GET");
                con.setDoInput(true);
                con.setDoOutput(true);
                con.connect();

                // Let's read the response
                StringBuffer buffer = new StringBuffer();
                is = con.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line = null;
                while ( (line = br.readLine()) != null )
                    buffer.append(line + "\r\n");

                is.close();
                con.disconnect();
                return buffer.toString();
            }
            catch(Throwable t) {
                t.printStackTrace();
            }
            finally {
                try { is.close(); } catch(Throwable t) {}
                try { con.disconnect(); } catch(Throwable t) {}
            }

            return null;
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            String data = (getWeatherData(params[0]));
            JSONObject json = new JSONObject();

            try {
                json = new JSONObject(data);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return json;
        }
    }
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accel(event);
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mag(event);
        }
        if (event.sensor.getType() == Sensor.TYPE_PRESSURE){
          float meters;
          float c;
          float pressurej = -1.0f;

          try {
            float temp = (float)(json.getJSONObject("main").getDouble("temp"));
            pressurej = (float)(json.getJSONObject("main").getInt("pressure"));
            /*float stdPressure = SensorManager.PRESSURE_STANDARD_ATMOSPHERE;

            float mmc = 1.3332239f;
            float ctemp = temp - 273.15f;

            c = (16 * (1 + (4 * ctemp)));
            meters = (c * ((stdPressure - pressure) * mmc)) / ((stdPressure + pressure) * mmc);*/

            } catch(Exception e) {
              e.getMessage();
            }


            if(historySize < 100){
               history.add(event.values[0]);
               historySize++;
            } else {
               history.remove();
               history.add(event.values[0]);
            }
            avgPressure = 0;
            for(Float f : history) {
              avgPressure += f;
            }
            if(pressurej != -1.0f) {
               altitude = (952 * ((pressurej - (avgPressure / historySize)) / 33.86f)) / 0.3048f;
            } else
                 Log.e("fetchData","fetchData");
            }
    }





}
