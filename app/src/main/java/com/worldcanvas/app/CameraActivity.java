package com.worldcanvas.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.view.LayoutInflater;
import android.widget.Toast;

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
        //preview.addView(glView);
        preview.addView(cameraPreview);

        final FrameLayout shade = (FrameLayout)findViewById(R.id.shade);
        final Animation animationFadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
        shade.startAnimation(animationFadeOut);
        final FrameLayout captureL = (FrameLayout) findViewById(R.id.capture);
        captureL.setVisibility(View.INVISIBLE);
        final Animation capture = AnimationUtils.loadAnimation(this, R.anim.fade_in);

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

        preview.setOnLongClickListener( new View.OnLongClickListener(){

            @Override
            public boolean onLongClick(View view) {
                captureL.startAnimation(capture);
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                // Vibrate for 500 milliseconds
                v.vibrate(500);
                return false;
            }
        });

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
