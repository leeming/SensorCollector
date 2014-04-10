package uk.ac.man.cs.leeming.sensorcollector;

import android.app.Service;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
    private boolean logging = false;

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

            //Only try to save cache if we are currently logging sensor data
            if(logging)
            {
                Log.i(TAG,"Thread iteration - dumping sensor values");
                saveCacheToFile();

                //cacheAcc.clear();
            }
        }
    }

    /**
     * Appends each sensor cache into its own file. Cache is then emptied
     * after saving to file. File is opened and closed each time this
     * method is called. TODO is this a problem?
     * If no params are given, all sensor caches are writen to file
     */
    public void saveCacheToFile()
    {
        Log.i(TAG,"Saving all cache");

        saveCacheToFile("accelerometer.csv",cacheAcc);
        saveCacheToFile("barometer.csv",cacheBar);
        saveCacheToFile("gravity.csv",cacheGrav);
        saveCacheToFile("gyroscope.csv",cacheGyro);
        saveCacheToFile("linear_acceleration.csv",cacheLinAcc);
        saveCacheToFile("rotation.csv",cacheRotation);
        saveCacheToFile("steps.csv",cacheStep);


    }

    public void saveCacheToFile(String filename, Queue<Pair<Long,String>> cache)
    {
        Log.i(TAG,"Saving cache to file => "+filename);
        //File fAcc = new File(saveTo,"accelerometer.csv");

        File file = new File(new File(workingDirectory), filename);

        try
        {
            //FileOutputStream f = new FileOutputStream(file);
            FileWriter f = new FileWriter(file,true);


            Pair<Long, String> s = null;
            while(cache.peek()!=null)
            {
                s=cache.remove();
                f.write(s.first+","+s.second+"\n");
            }

            f.flush();
            f.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed to "+filename+" : " + e.toString());
        }
    }

    /**
     * Stop logging sensor data. This also saves what ever data is
     * left in cache to file and unregisters the listeners
     */
    public void stopLogging() {
        //stump
        Log.i(TAG, "Stopping collection");
        logging = false;

        saveCacheToFile();

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

    public void startLogging(String filename,int delay) {
        Log.i(TAG,"Waiting "+delay+" seconds until start");
        while(delay>0)
        {
            SystemClock.sleep(1000);
            delay--;

            //Play a beep noise
            if(delay==0)    //long
            {
                final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
                tg.startTone(ToneGenerator.TONE_PROP_BEEP,1000);
                Log.v(TAG,"BEEEEEEEPPPPP");
            }
            else    //short
            {
                final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
                tg.startTone(ToneGenerator.TONE_PROP_BEEP,100);
                tg.stopTone();
                Log.v(TAG,"BEEP");
            }
        }
        startLogging(filename);
    }
    public void startLogging(String filename) {
        //stump
        Log.i(TAG, "Starting collection");
        logging = true;

        //File f = activityRef.getFilesDir();


        workingDirectory = Environment.getExternalStorageDirectory().getAbsolutePath() + "/experimentdata/"+filename+"_"+System.currentTimeMillis();

        saveTo = new File(workingDirectory);
        saveTo.mkdirs();
        Log.i(TAG,"App file root at: "+workingDirectory);


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
        cacheBar.add(Pair.create(System.nanoTime(), String.valueOf(event.values[0])));
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
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        StringBuilder r = new StringBuilder();
        r.append(Float.valueOf(x)).append(",");
        r.append(Float.valueOf(y)).append(",");
        r.append(Float.valueOf(z)).append(",");
        r.append(Float.valueOf(event.values[3]));

        cacheRotation.add(Pair.create(System.nanoTime(), r.toString()));
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