package com.ccs.app.maps;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import static android.hardware.Sensor.TYPE_ROTATION_VECTOR;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    SensorManager sensorManager;

    float[] mags, accels;

    View imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.image_view);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        startActivity(new Intent(this, MapsActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // register this class as a listener for the orientation and
        // accelerometer sensors
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        // unregister listener
        super.onPause();
        sensorManager.unregisterListener(this);
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
            final float[] R = new float[16];
            final float[] I = new float[9];

            float[] values = new float[3];

            SensorManager.getRotationMatrix(R, I, accels, mags);

            SensorManager.getOrientation(R, values);

//            Log.d("onSensorChanged", "0:" + R[0] + ", 1:" + R[1] + ", 2:" + R[2] + ", 3:" + R[3] + ", 4:" + R[4] + ", 5:" + R[5] + ", 6:" + R[6] + ", 7:" + R[7] + ", 8:" + R[8]);
//            Log.d("onSensorChanged", "0:" + ((int) (Math.acos(R[0]) * 180 / Math.PI)) + ", 1:" + ((int) (Math.acos(R[1]) * 180 / Math.PI)) + ", 2:" + ((int) (Math.acos(R[2]) * 180 / Math.PI))
//                    + ",======, 3:" + ((int) (Math.acos(R[3]) * 180 / Math.PI)) + ", 4:" + ((int) (Math.acos(R[4]) * 180 / Math.PI)) + ", 5:" + ((int) (Math.acos(R[5]) * 180 / Math.PI))
//                    + ",======, 6:" + ((int) (Math.acos(R[6]) * 180 / Math.PI)) + ", 7:" + ((int) (Math.acos(R[7]) * 180 / Math.PI)) + ", 8:" + ((int) (Math.acos(R[8]) * 180 / Math.PI)));

            Log.d("onSensorChanged", "" + ((int) (Math.acos(R[8]) * 180 / Math.PI)));

            float az = (float) (Math.acos(R[10]) * 180 / Math.PI);

            if(Math.abs(az) < 30) return;

            float x = R[8], y = R[9];

            float g = (float) (Math.atan2(x,y) * 180 / Math.PI);

            float dg = g - imageView.getRotation();

            if(Math.abs(dg) > 180) {
                float dg2 = (360 - Math.abs(dg));
                dg2 *= dg > 0 ? -1 : 1;
                dg = dg2;
            }

            dg /= 10;

            imageView.setRotation(imageView.getRotation() + dg);

//            Log.d("onSensorChanged", "" + ((int) g));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d("maps", "onAccuracyChanged");

    }

}
