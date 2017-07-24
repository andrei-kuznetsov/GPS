package gps.com.gps;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * Created by Ира on 18.06.2017.
 */

public class GPSTracker extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, LocationService.class)); // to start the service that does the work
    }
}