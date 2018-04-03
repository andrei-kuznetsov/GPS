package kspt.revkina.gps;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Class for get locations
 */
class UserLocation {

    public void getLastLocation(Context context, final GoogleMap googleMap) {
        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        Cursor cursor = database.rawQuery("select * from " + DBHelper.TABLE + " order by " + DBHelper.KEY_ID + " DESC limit 1", null);
        cursor.moveToFirst();
        double latitude = cursor.getDouble(cursor.getColumnIndex(DBHelper.KEY_LAT));
        double longitude = cursor.getDouble(cursor.getColumnIndex(DBHelper.KEY_LON));
        cursor.close();
        LatLng latLng = new LatLng(latitude, longitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
    }

    public void setupClaster(final Context context, final GoogleMap googleMap) {
        final ClusterManager<MyItem> mClusterManager = new ClusterManager<>(context, googleMap);
        googleMap.setOnCameraChangeListener(mClusterManager);
        googleMap.setOnMarkerClickListener(mClusterManager);
        new Thread(
                new Runnable() {
                    public void run() {
                        addItems(context, mClusterManager);
                    }
                }
        ).start();
    }

    void addItems(Context context, ClusterManager<MyItem> mClusterManager) {
        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        long currentMillis = System.currentTimeMillis();
        long hoursInterval = 60 * 60 * 1000;

        Cursor cursor = database.rawQuery("select * from " + DBHelper.TABLE +
                " where (" + currentMillis + " - " + DBHelper.KEY_DATE + ")/(" + hoursInterval + ") < 24", null);

        while (cursor.moveToNext()) {
            double latitude = cursor.getDouble(cursor.getColumnIndex(DBHelper.KEY_LAT));
            double longitude = cursor.getDouble(cursor.getColumnIndex(DBHelper.KEY_LON));
            MyItem offsetItem = new MyItem(latitude, longitude);
            mClusterManager.addItem(offsetItem);
        }
        cursor.close();
    }
}
