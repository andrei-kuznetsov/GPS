package gps.com.gps;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;


/**
 * Служба для получения данных и занисения их в БД
 */
public class LocationService extends Service implements
//This callback will have a public function onConnected() which will be called whenever device is connected and disconnected.
        GoogleApiClient.ConnectionCallbacks,
//Provides callbacks for scenarios that result in a failed attempt to connect the client to the service.
// Whenever connection is failed onConnectionFailed() will be called.
        GoogleApiClient.OnConnectionFailedListener,
//This callback will be called whenever there is change in location of device. Function onLocationChanged() will be called.
        LocationListener {

    private static final long INTERVAL = 1000 * 60 * 2; //2 minute
    private static final long FASTEST_INTERVAL = 1000 * 60 * 1; // 1 minute

    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest; //used to get quality of service for location updates from the FusedLocationProviderApi using requestLocationUpdates
    DBHelper dbHelper;//Создание БД


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // if we are currently trying to get a location and the alarm manager has called this again,
        // no need to start processing a new location.

        startTracking();

        return START_NOT_STICKY; //Этот режим используется в сервисах, которые запускаются для выполнения конкретных действий или команд
    }

    private void startTracking() {
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
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
        stopSelf(); //Служба может остановить сама себя
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            //требуемая точность 500 метров, поэтому мы можем отказаться от этой услуги
            //onDestroy будет вызываться и останавливать наше местоположение
            if (location.getAccuracy() < 10) {
                stopLocationUpdates();
            } else {
                dbHelper = new DBHelper(getApplicationContext());
                dbHelper.createNewTable(location.getLatitude(), location.getLongitude(), location.getAccuracy(),
                        location.getProvider(), batteryLevel());
            }
        }

    }


    /**
     * use this to get the last updated location.
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {

        PendingResult<Status> pendingResult = LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    private void stopLocationUpdates() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }
    public String batteryLevel() {
        Intent intent  = this.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int    level   = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int    scale   = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int    percent = (level*100)/scale;
        return String.valueOf(percent) + "%";
    }

}
