package kspt.revkina.gps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;

public class TransitionIntentService extends BroadcastReceiver {
    private LocationService locationService = new LocationService();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ActivityTransitionResult.hasResult(intent)) {
            ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
            SharedPreferences  pref = PreferenceManager.getDefaultSharedPreferences(context);
            for (ActivityTransitionEvent event : result.getTransitionEvents()) {
                switch (event.getActivityType()){
                    case DetectedActivity.IN_VEHICLE:
                        locationService.updateLocationRequest(Long.parseLong(pref.getString("vehicle_seconds", "500")),
                                Float.parseFloat(pref.getString("vehicle_meters", "1000")));
                        break;
                    case DetectedActivity.ON_BICYCLE:
                        locationService.updateLocationRequest(Long.parseLong(pref.getString("bicycle_seconds", "500")),
                                Float.parseFloat(pref.getString("bicycle_meters", "500")));
                        break;
                    case DetectedActivity.STILL:
                        locationService.updateLocationRequest(Long.parseLong(pref.getString("still_seconds", "1000")),
                                Float.parseFloat(pref.getString("still_meters", "1000")));
                        break;
                    case DetectedActivity.WALKING:
                        locationService.updateLocationRequest(Long.parseLong(pref.getString("walking_seconds", "120")),
                                Float.parseFloat(pref.getString("walking_meters", "10")));
                        break;
                    case DetectedActivity.RUNNING:
                        locationService.updateLocationRequest(Long.parseLong(pref.getString("running_seconds", "60")),
                                Float.parseFloat(pref.getString("running_meters", "100")));
                        break;
                        default:
                            locationService.updateLocationRequest(120, 10);
                            break;
                }
            }
        }
    }
}