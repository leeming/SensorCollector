package uk.ac.man.cs.leeming.sensorcollector;

import android.app.Service;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by leeming on 08/04/14.
 */
public class SensorLogger extends Thread implements SensorEventListener {
    private final String TAG = "LoggerThread";
    MainActivity activityRef;
    private boolean alive = true;

    private SensorManager mSensorManager;

    /**
     * Buffers for all the sensor data. Time is from System.nanoTime() so not
     * wall clock time but time since boot and is the most accurate timer.
     */
    Queue<Pair<Long, String>> cacheAcc;
    Queue<Pair<Long, String>> cacheGrav;
    Queue<Pair<Long, String>> cacheLinAcc;
    Queue<Pair<Long, String>> cacheGyro;
    Queue<Pair<Long, String>> cacheStep;
    Queue<Pair<Long, String>> cacheRotation;
    Queue<Pair<Long, String>> cacheBar;
    private String workingDirectory;
    private File saveTo;

    public SensorLogger(MainActivity activityRef) {
        this.activityRef = activityRef;
        mSensorManager = (SensorManager) activityRef.getSystemService(Service.SENSOR_SERVICE);

        cacheAcc = new LinkedList<Pair<Long, String>>();
        cacheGrav = new LinkedList<Pair<Long, String>>();
        cacheLinAcc = new LinkedList<Pair<Long, String>>();
        cacheGyro = new LinkedList<Pair<Long, String>>();
        cacheStep = new LinkedList<Pair<Long, String>>();
        cacheRotation = new LinkedList<Pair<Long, String>>();
        cacheBar = new LinkedList<Pair<Long, String>>();
    }

    public void run() {
        while (alive) {

            SystemClock.sleep(5000);

            Log.i(TAG,"Thread iteration - dumping sensor values");

            saveCacheToFile();

            //Log.i(TAG,cacheAcc.toString()); //todo cacheACC isn't concurrent

            cacheAcc.clear();
        }
    }

    public void saveCacheToFile()
    {
        File fAcc = new File(saveTo,"accelerometer.csv");


        try
        {

            FileWriter out = new FileWriter(fAcc);


            Pair<Long, String> s = null;
            while(cacheAcc.peek()!=null)
            {
                s=cacheAcc.remove();
                Log.v(TAG,s.first+":"+s.second);
                out.write(s.first+","+s.second+"\n");
            }


            out.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public void stopLogging() {
        //stump
        Log.i(TAG, "Stopping collection");

        //Stops listening to ALL sensors
        mSensorManager.unregisterListener(this);

    }

    //Define all the sensor types and sampling rates
    int[][] toLog = {
            {Sensor.TYPE_ACCELEROMETER, SensorManager.SENSOR_DELAY_FASTEST},
            {Sensor.TYPE_GRAVITY, SensorManager.SENSOR_DELAY_FASTEST},
            {Sensor.TYPE_GYROSCOPE, SensorManager.SENSOR_DELAY_FASTEST},
            {Sensor.TYPE_STEP_DETECTOR, SensorManager.SENSOR_DELAY_FASTEST},
            {Sensor.TYPE_LINEAR_ACCELERATION, SensorManager.SENSOR_DELAY_FASTEST},
            {Sensor.TYPE_ROTATION_VECTOR, SensorManager.SENSOR_DELAY_NORMAL},
            {Sensor.TYPE_PRESSURE, SensorManager.SENSOR_DELAY_NORMAL}
    };

    public void startLogging(String filename) {
        //stump
        Log.i(TAG, "Starting collection");

        File f = activityRef.getFilesDir();
        Log.i(TAG,"App file root at: "+f.getAbsolutePath());

        workingDirectory = filename+"_"+System.currentTimeMillis();

        Log.i(TAG,"PWD : "+workingDirectory);

        saveTo = new File(f,workingDirectory);
        saveTo.mkdir();


        //Add all the listeners
        for (int i = 0; i < toLog.length; i++) {
            mSensorManager.registerListener(this,
                    mSensorManager.getDefaultSensor(toLog[i][0]),
                    toLog[i][1]
            );
        }


    }

    public void kill() {


        alive = false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //Figure out which sensor triggered this event
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                getAcc(event);
                break;
            case Sensor.TYPE_GYROSCOPE:
                getGyro(event);
                break;
            case Sensor.TYPE_STEP_DETECTOR:
                getStep(event);
                break;
            case Sensor.TYPE_GRAVITY:
                getGravity(event);
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                getLinAcc(event);
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                getRotation(event);
                break;
            case Sensor.TYPE_PRESSURE:
                getBar(event);
                break;

        }
    }

    /*
     * All the following are sensor specific recording methods. Each sensor
     * values are defined at
     * https://developer.android.com/guide/topics/sensors/sensors_motion.html
     */

    private void getBar(SensorEvent event) {
        //athmospheric pressure in hectopascal (hPa)
    }

    private void getGravity(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        StringBuilder r = new StringBuilder();
        r.append(Float.valueOf(x)).append(",");
        r.append(Float.valueOf(y)).append(",");
        r.append(Float.valueOf(z));

        cacheGrav.add(Pair.create(System.nanoTime(), r.toString()));
    }

    private void getLinAcc(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        StringBuilder r = new StringBuilder();
        r.append(Float.valueOf(x)).append(",");
        r.append(Float.valueOf(y)).append(",");
        r.append(Float.valueOf(z));

        cacheLinAcc.add(Pair.create(System.nanoTime(), r.toString()));
    }

    private void getStep(SensorEvent event) {
        cacheStep.add(Pair.create(System.nanoTime(), "1"));
    }

    private void getRotation(SensorEvent event) {
    }

    private void getGyro(SensorEvent event) {
        float xRoll = event.values[2];
        float yPitch = event.values[1];
        float zYaw = event.values[0];

        StringBuilder r = new StringBuilder();
        r.append(Float.valueOf(xRoll)).append(",");
        r.append(Float.valueOf(yPitch)).append(",");
        r.append(Float.valueOf(zYaw));

        cacheGyro.add(Pair.create(System.nanoTime(), r.toString()));
    }

    private void getAcc(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        StringBuilder r = new StringBuilder();
        r.append(Float.valueOf(x)).append(",");
        r.append(Float.valueOf(y)).append(",");
        r.append(Float.valueOf(z));

        cacheAcc.add(Pair.create(System.nanoTime(), r.toString()));
    }
}