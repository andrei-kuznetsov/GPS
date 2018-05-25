package kspt.revkina.gps;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for retrieving data and putting them into a database
 */
public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final long INTERVAL = 1000 * 60;
    private static final long FASTEST_INTERVAL = 1000 * 60;
    private float meters = 10;
    SharedPreferences pref;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    DBHelper dbHelper;

    private ActivityRecognitionClient activityRecognitionClient;
    private PendingIntent transitionPendingIntent;

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new DBHelper(getApplicationContext());
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        meters = Float.parseFloat(pref.getString(getString(R.string.meters), "10"));
        startService(new Intent(this, TransitionIntentService.class));

        activityRecognitionClient = ActivityRecognition.getClient(getApplicationContext());
        Intent intent = new Intent(getApplicationContext(), TransitionIntentService.class);
        transitionPendingIntent = PendingIntent.getService(getApplicationContext(), 100, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startTracking();
        return START_NOT_STICKY;
    }

    private void startTracking() {
        if ( GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addApi(ActivityRecognition.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            if (!mGoogleApiClient.isConnected() || !mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        createLocationRequest();
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        stopLocationUpdates();
        stopSelf();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            location.setAccuracy(meters);
            dbHelper.createNewNote(location.getLatitude(), location.getLongitude(), location.getAccuracy(),
                    location.getTime(), location.getProvider(), batteryLevel());
        }

    }

    protected void updateLocationRequest(long interval, float metersNew) {
        mLocationRequest.setInterval(interval*INTERVAL);
        meters = metersNew;
    }

    /**
     * use this to get the last updated location.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(Long.parseLong(pref.getString(getString(R.string.seconds), "120"))*INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
        if (pref.getBoolean("enter", true)) {
            if (pref.getBoolean("active_auto", false)) {
                deregisterActivityTransitionUpdates();
            } else {
                requestActivityTransitionUpdates();
            }
        }
    }

    private void stopLocationUpdates() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    private String batteryLevel() {
        Intent intent  = this.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        assert intent != null;
        int    level   = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int    scale   = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int    percent = (level*100)/scale;
        return String.valueOf(percent) + "%";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dbHelper.close();
        deregisterActivityTransitionUpdates();
    }

    private void deregisterActivityTransitionUpdates() {
        Task<Void> task = activityRecognitionClient.removeActivityTransitionUpdates(transitionPendingIntent);
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                transitionPendingIntent.cancel();
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void requestActivityTransitionUpdates() {
        ListTransition listTransition = new ListTransition();
        ActivityTransitionRequest request = listTransition.buildTransitionRequest();

        Task<Void> task = activityRecognitionClient.requestActivityTransitionUpdates(request,
                transitionPendingIntent);

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e("taskTransitionFail", e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
