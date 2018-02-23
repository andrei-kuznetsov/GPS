package kspt.revkina.gps;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterManager;

import java.text.SimpleDateFormat;

/**
 * Class for get locations
 */
class UserLocation {

    public LatLng getLastLocation(Context context) {
        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        Cursor cursor = database.rawQuery("select * from " + DBHelper.TABLE + " order by " + DBHelper.KEY_ID + " limit 1", null);
        cursor.moveToFirst();
        double latitude = cursor.getDouble(cursor.getColumnIndex(DBHelper.KEY_LAT));
        double longitude = cursor.getDouble(cursor.getColumnIndex(DBHelper.KEY_LON));
        LatLng latLng = new LatLng(latitude, longitude);
        cursor.close();
        return latLng;
    }

    public void setupClaster(Context context, GoogleMap googleMap) {
        ClusterManager<MyItem> mClusterManager = new ClusterManager<>(context, googleMap);
        googleMap.setOnCameraChangeListener(mClusterManager);
        googleMap.setOnMarkerClickListener(mClusterManager);
        addItems(context, mClusterManager);
    }

    void addItems(Context context, ClusterManager<MyItem> mClusterManager) {
        DBHelper dbHelper = new DBHelper(context);
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        String dateKontrol = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(System.currentTimeMillis());

        Cursor cursor = database.query(DBHelper.TABLE,
                new String[]{DBHelper.KEY_ID, DBHelper.KEY_DATE, DBHelper.KEY_LAT, DBHelper.KEY_LON},
                DBHelper.KEY_DATE+"=?", new String[]{dateKontrol}, null, null,
                DBHelper.KEY_DATE + " DESC");

        while (cursor.moveToNext()) {
            double latitude = cursor.getDouble(cursor.getColumnIndex(DBHelper.KEY_LAT));
            double longitude = cursor.getDouble(cursor.getColumnIndex(DBHelper.KEY_LON));
            MyItem offsetItem = new MyItem(latitude, longitude);
            mClusterManager.addItem(offsetItem);
        }
        cursor.close();
    }
}
