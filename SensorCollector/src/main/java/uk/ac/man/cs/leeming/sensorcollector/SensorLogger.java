package uk.ac.man.cs.leeming.sensorcollector;

import android.util.Log;

/**
 * Created by leeming on 08/04/14.
 */
public class SensorLogger extends Thread
{
    private final String TAG = "LoggerThread";

    private boolean alive = true;

    public void run()
    {
        while(alive)
        {}
    }

    public void stopLogging()
    {
        //stump
        Log.i(TAG, "Stopping collection");
    }

    public void startLogging()
    {
        //stump
        Log.i(TAG, "Starting collection");
    }

    public void kill()
    {
        alive=false;
    }
}
