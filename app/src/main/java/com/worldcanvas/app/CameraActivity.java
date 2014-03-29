package com.worldcanvas.app;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

/**
 * Created by Mario on 3/23/2014.
 */
public class CameraActivity extends Activity {
    private Camera camera;
    private CameraPreview cameraPreview;
    private GLSurfaceView glView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_preview);

        Log.i("run", "run");

        camera = getCameraInstance();
        camera.setDisplayOrientation(90);
        cameraPreview = new CameraPreview(this, camera);

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
        super.onPause();
        releaseCamera();
        glView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        glView.onResume();
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
}
