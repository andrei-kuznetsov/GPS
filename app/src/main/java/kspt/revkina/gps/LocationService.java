package kspt.revkina.gps;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/**
 * Service for retrieving data and putting them into a database
 */
public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final long INTERVAL = 1000;
    private static final long FASTEST_INTERVAL = 1000 * 5;
    public static final String KEY_SECONDS = "seconds";
    private static final String KEY_ACCURACY = "meters";
    private static final int[] PRIORITIES = {
            LocationRequest.PRIORITY_HIGH_ACCURACY,
            LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY,
            LocationRequest.PRIORITY_LOW_POWER,
            LocationRequest.PRIORITY_NO_POWER
    };

    SharedPreferences pref;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    DBHelper dbHelper;

    private ActivityRecognitionClient activityRecognitionClient;
    private PendingIntent transitionPendingIntent;
    private NotificationCompat.Builder nfBuilder;

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new DBHelper(getApplicationContext());
        pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        activityRecognitionClient = ActivityRecognition.getClient(getApplicationContext());
        Intent intent = new Intent(getApplicationContext(), TransitionIntentService.class);
        transitionPendingIntent = PendingIntent.getService(getApplicationContext(), 100, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        nfBuilder = new NotificationCompat.Builder(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createLocationRequest();

        if (mGoogleApiClient == null) {
            connectGoogleServices();
        } else {
            startLocationUpdates();
        }

        return START_STICKY;
    }

    /* Used to build and start foreground service. */
    private void setForeground() {
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle("Location service is running");
        bigTextStyle.bigText(String.valueOf(mLocationRequest));

        nfBuilder.setStyle(bigTextStyle);
        nfBuilder.setWhen(System.currentTimeMillis());
        nfBuilder.setSmallIcon(R.mipmap.gps);

        // Build the notification.
        Notification notification = nfBuilder.build();

        // Start foreground service.
        startForeground(1, notification);
    }

    private void connectGoogleServices() {
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addApi(ActivityRecognition.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            if (!mGoogleApiClient.isConnected() || !mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        } else {
            //Warning about connect fail
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
        disconnectGoogleClient();
        stopSelf();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
//            FIXME: this does not work
//            updateSpeedOrTime(location.getSpeed());
            dbHelper.createNewRecord(location.getLatitude(), location.getLongitude(), location.getAccuracy(),
                    location.getTime(), location.getProvider(), batteryLevel());
            Log.d("location", "received location: " + location);
        }
    }

    private void updateSpeedOrTime(Float speed) {
        if (pref.getBoolean("enter", true)) {
            SharedPreferences pref = getApplicationContext().getSharedPreferences("list", 0);
            ArrayList<HashMap<String, String>> mList = new ArrayList<>();
            ConversionListGson conversionListGson = new ConversionListGson();

            if (pref.getBoolean("speed_auto", false)) {
                String storedCollection = pref.getString("speed", null);

                if (storedCollection != null) {
                    mList = conversionListGson.getSpeedList("speed", pref, mList);
                }

                for (int i = 0; i < mList.size(); i++) {
                    HashMap<String, String> index = mList.get(i);
                    Float speedFrom = Float.valueOf(index.get("speedFrom"));
                    Float speedBefore = Float.valueOf(index.get("speedBefore"));
                    if (speedFrom >= speed && speed <= speedBefore) {
                        updateLocationRequest(Long.parseLong(index.get("seconds")), Integer.parseInt(index.get("meters")));
                    }
                }
            } else if (pref.getBoolean("time_auto", false)) {
                String storedCollection = pref.getString("time", null);

                if (storedCollection != null) {
                    mList = conversionListGson.getSpeedList("time", pref, mList);
                }
                String currentTime = DateUtils.formatDateTime(this,
                        Calendar.getInstance().getTimeInMillis(), DateUtils.FORMAT_SHOW_TIME);

                for (int i = 0; i < mList.size(); i++) {
                    HashMap<String, String> index = mList.get(i);
                    String timeFrom = String.valueOf(index.get("timeFrom"));
                    String timeBefore = String.valueOf(index.get("timeBefore"));
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                    try {
                        Date time1 = sdf.parse(timeFrom);
                        Date time2 = sdf.parse(timeBefore);
                        Date current = sdf.parse(currentTime);
                        if (current.after(time1) && current.before(time2)) {
                            updateLocationRequest(Long.parseLong(index.get("seconds")), Integer.parseInt(index.get("meters")));
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    protected void updateLocationRequest(long intervalInSec, int priority) {
        mLocationRequest.setInterval(intervalInSec * INTERVAL);
        mLocationRequest.setPriority(priority);
    }

    /**
     * use this to get the last updated location.
     */
    private void createLocationRequest() {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        long interval = Long.parseLong(pref.getString(KEY_SECONDS, "120"));
        int priorityIdx = Integer.parseInt(pref.getString(KEY_ACCURACY, "0"));
        ;
        int priority = PRIORITIES[priorityIdx];
        updateLocationRequest(interval, priority);
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

        Toast.makeText(this, "Service is started: " + mLocationRequest, Toast.LENGTH_LONG).show();
        setForeground();
    }

    private void disconnectGoogleClient() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        mGoogleApiClient = null;
    }

    private void stopLocationUpdates() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    private String batteryLevel() {
        Intent intent = this.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        assert intent != null;
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int percent = (level * 100) / scale;
        return String.valueOf(percent) + "%";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dbHelper.close();
        deregisterActivityTransitionUpdates();
        stopLocationUpdates();
        disconnectGoogleClient();
        Toast.makeText(this, "Service is stopeed", Toast.LENGTH_SHORT).show();
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