package com.ccs.app.maps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.ccs.app.maps.camera.Preview;
import com.ccs.app.maps.config.Debug;

public class CameraActivity extends AppCompatActivity implements SensorEventListener {

    private final String TAG = getClass().getSimpleName();

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    Preview preview;
    Button buttonClick;
    Camera camera;
    Activity act;
    Context ctx;

    SensorManager sensorManager;

    float[] mags, accels;

    float rotation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_camera);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);




        ctx = this;
        act = this;

        preview = new Preview(this, (SurfaceView) findViewById(R.id.surfaceView));
        preview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        ((FrameLayout) findViewById(R.id.root)).addView(preview);
        preview.setKeepScreenOn(true);

        preview.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                camera.takePicture(null, null, jpegCallback);
            }
        });

        Toast.makeText(ctx, getString(R.string.take_photo_help), Toast.LENGTH_LONG).show();

    }

    @Override
    protected void onResume() {
        super.onResume();
        int numCams = Camera.getNumberOfCameras();
        if(numCams > 0){
            try{
                camera = Camera.open(0);
                camera.startPreview();
                preview.setCamera(camera);
            } catch (RuntimeException ex){
                Toast.makeText(ctx, getString(R.string.camera_not_found), Toast.LENGTH_LONG).show();
            }
        }

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        if(camera != null) {
            camera.stopPreview();
            preview.setCamera(null);
            camera.release();
            camera = null;
        }
        sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
//        Log.d("onSensorChanged", "" + event.sensor.getType());

        switch(event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                accels = event.values.clone();
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                mags = event.values.clone();
                break;
        }

//        Log.d("onSensorChanged", "" + (mags==null) + "," + (accels==null));

        if (mags != null && accels != null) {
            final float[] R = new float[9];
            final float[] I = new float[9];

            float[] values = new float[3];

            SensorManager.getRotationMatrix(R, I, accels, mags);

            SensorManager.getOrientation(R, values);

//            Log.d("onSensorChanged", "0:" + R[0] + ", 1:" + R[1] + ", 2:" + R[2] + ", 3:" + R[3] + ", 4:" + R[4] + ", 5:" + R[5] + ", 6:" + R[6] + ", 7:" + R[7] + ", 8:" + R[8]);
//            Log.d("onSensorChanged", "0:" + ((int) (Math.acos(R[0]) * 180 / Math.PI)) + ", 1:" + ((int) (Math.acos(R[1]) * 180 / Math.PI)) + ", 2:" + ((int) (Math.acos(R[2]) * 180 / Math.PI))
//                    + ",======, 3:" + ((int) (Math.acos(R[3]) * 180 / Math.PI)) + ", 4:" + ((int) (Math.acos(R[4]) * 180 / Math.PI)) + ", 5:" + ((int) (Math.acos(R[5]) * 180 / Math.PI))
//                    + ",======, 6:" + ((int) (Math.acos(R[6]) * 180 / Math.PI)) + ", 7:" + ((int) (Math.acos(R[7]) * 180 / Math.PI)) + ", 8:" + ((int) (Math.acos(R[8]) * 180 / Math.PI)));

//            Log.d("onSensorChanged", "" + ((int) (Math.acos(R[8]) * 180 / Math.PI)));

//            float az = (float) (Math.acos(R[8]) * 180 / Math.PI);
//
//            if(Math.abs(az) < 30) return;

            float x = R[6], y = R[7];

            rotation = (float) (Math.atan2(-x,y) * 180 / Math.PI) +90;

//            Log.d("onSensorChanged", "" + ((int) rotation));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(Debug.TAG + TAG, "onAccuracyChanged");

    }

    private PictureCallback mPicture = new PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };


    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    private void resetCam() {
        camera.startPreview();
        preview.setCamera(camera);
    }

    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
            //			 Log.d(TAG, "onShutter'd");
        }
    };

    Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            //			 Log.d(TAG, "onPictureTaken - raw");
        }
    };

    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            new SaveImageTask().execute(data);
            resetCam();
            Log.d(TAG, "onPictureTaken - jpeg");
        }
    };

    private class SaveImageTask extends AsyncTask<byte[], Void, Void> {

        @Override
        protected Void doInBackground(byte[]... data) {
            FileOutputStream outStream, outStream90;

            // Write to SD Card
            try {
                File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File (sdCard.getAbsolutePath() + "/CamTest");
                dir.mkdirs();

                String fileName = String.format("%d.jpg", System.currentTimeMillis());
                File outFile = new File(dir, fileName);


                outStream = new FileOutputStream(new File(dir, fileName));

//                int rotation = CameraActivity.this.getWindowManager().getDefaultDisplay().getRotation();
//                switch (rotation) {
//                    case Surface.ROTATION_0:
//                        rotation = 0;
//                        break;
//                    case Surface.ROTATION_90:
//                        rotation = 90;
//                        break;
//                    case Surface.ROTATION_180:
//                        rotation = 180;
//                        break;
//                    case Surface.ROTATION_270:
//                        rotation = 270;
//                        break;
//                }

                int rotate = 0;

                if(rotation >= -45 && rotation < 45) rotate = 0;
                else if(rotation >= 45 && rotation < 135) rotate = 90;
                else if(rotation >= 135 && rotation < 225) rotate = 180;
                else if(rotation >= 225 && rotation < 315) rotate = 270;
//                else if(rotation >= 315 && rotation < 405) rotate = 360;
                else if(rotation >= -135 && rotation < -45) rotate = -90;
                else if(rotation >= -225 && rotation < -135) rotate = -180;
                else if(rotation >= -315 && rotation < -225) rotate = -270;


                Bitmap bitmap = BitmapFactory.decodeByteArray(data[0], 0, data[0].length);
                rotate(bitmap, (int) rotate).compress(Bitmap.CompressFormat.JPEG, 100, outStream);

//                outStream.write(data[0]);
//                outStream.flush();

                outStream.close();

                Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to " + outFile.getAbsolutePath());

                refreshGallery(outFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
            }
            return null;
        }

    }

    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        //       mtx.postRotate(degree);
        mtx.setRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    public static int getRotationAngle(Activity mContext, int cameraId) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = mContext.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

//    Camera mCamera;
//    Preview mPreview;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//
//        setContentView(R.layout.activity_camera);
//
//        mPreview = findViewById(R.id.preview);
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
//            Camera.CameraInfo info = new Camera.CameraInfo();
//            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
//                Camera.getCameraInfo(i, info);
//                if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
//                    if(safeCameraOpen(i)) {
//                        Log.d(Debug.TAG + TAG, "Camera opened");
//
//                        new Handler().postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
//                                mPreview.setCamera(mCamera);
//                            }
//                        }, 1000);
//                    }
//                }
//            }
//        }
//    }
//
//    private boolean safeCameraOpen(int id) {
//        boolean qOpened = false;
//
//        try {
//            releaseCameraAndPreview();
//            mCamera = Camera.open(id);
//            qOpened = (mCamera != null);
//        } catch (Exception e) {
//            Log.e(Debug.TAG + TAG, "failed to open Camera");
//            e.printStackTrace();
//        }
//
//        return qOpened;
//    }
//
//    private void releaseCameraAndPreview() {
//        mPreview.setCamera(null);
//        if (mCamera != null) {
//            mCamera.release();
//            mCamera = null;
//        }
//    }

}
