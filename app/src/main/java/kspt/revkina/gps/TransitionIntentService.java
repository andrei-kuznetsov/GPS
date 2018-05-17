package kspt.revkina.gps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;

public class TransitionIntentService extends BroadcastReceiver {
    private LocationService locationService = new LocationService();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ActivityTransitionResult.hasResult(intent)) {
            ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
            for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                switch (event.getActivityType()){
                    case DetectedActivity.IN_VEHICLE:
                        locationService.updateLocationRequest(500, 1000);
                        break;
                    case DetectedActivity.ON_BICYCLE:
                        locationService.updateLocationRequest(500, 500);
                        break;
                    case DetectedActivity.STILL:
                        locationService.updateLocationRequest(1000, 1000);
                        break;
                    case DetectedActivity.WALKING:
                        locationService.updateLocationRequest(120, 10);
                        break;
                    case DetectedActivity.RUNNING:
                        locationService.updateLocationRequest(60, 100);
                        break;
                        default:
                            locationService.updateLocationRequest(120, 10);
                            break;
                }
                Toast.makeText(context, event.getTransitionType() + "-" + event.getActivityType(), Toast.LENGTH_LONG).show();
            }
        }
    }
}