package kspt.revkina.gps;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.clustering.ClusterManager;

import android.view.View.OnClickListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


/**
 * Application interface
 */
public class MapsActivity extends FragmentActivity implements OnClickListener, OnMapReadyCallback {

    private GoogleMap googleMap;
    private SQLiteDatabase database;
    private DBHelper dbHelper;
    private ImageButton img;
    private ClusterManager<MyItem> mClusterManager;
    private static final String APP_PREFERENCES = "settings";
    private static final String APP_PREFERENCES_METRES = "metres";
    private static final String APP_PREFERENCES_TIME = "time";
    private SharedPreferences sharedPrefs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPrefs = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        dbHelper = new DBHelper(getApplicationContext());
        setContentView(R.layout.activity_location_google_map);
        Button btnInfo = (Button) findViewById(R.id.Information);
        btnInfo.setOnClickListener(this);
        img = (ImageButton) findViewById(R.id.Setting);
        img.setOnClickListener(this);

        SupportMapFragment fm = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        fm.getMapAsync(this);
        database = dbHelper.getWritableDatabase();

    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        googleMap.setMyLocationEnabled(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(dbHelper.countBD()==0)
            img.setEnabled(false);
        else
            img.setEnabled(true);
        startTracker();
    }

    private void startTracker() {
        if (!isGooglePlayServicesAvailable()) {
            return;
        }
        Intent gpsTrackerIntent = new Intent(getBaseContext(), GPSTracker.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(), 0, gpsTrackerIntent, 0);
        AlarmManager alarmManager = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
        int seconds = 120;
        if(sharedPrefs.contains(APP_PREFERENCES_TIME)){
            seconds = sharedPrefs.getInt(APP_PREFERENCES_TIME, 120);
        }
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(),
                10000 * seconds,
                pendingIntent);

        if (dbHelper.countBD()!=0)
            setupClusterer();

    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GoogleApiAvailability.getInstance().getErrorDialog(this, status, 0).show();
            return false;
        }
    }



    private void setupClusterer() {
        Cursor zero = database.rawQuery("select * from " + DBHelper.TABLE + " where " + DBHelper.KEY_ID + "=" +
                dbHelper.countBD(), null);
        zero.moveToFirst();

        double latitude = Double.parseDouble(zero.getString(zero.getColumnIndex(DBHelper.KEY_LAT)));
        double longitude = Double.parseDouble(zero.getString(zero.getColumnIndex(DBHelper.KEY_LON)));
        zero.close();
        int metres = 10;
        if(sharedPrefs.contains(APP_PREFERENCES_METRES)){
            metres = sharedPrefs.getInt(APP_PREFERENCES_METRES, 10);
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), metres));

        mClusterManager = new ClusterManager<>(this, googleMap);
        googleMap.setOnCameraChangeListener(mClusterManager);
        googleMap.setOnMarkerClickListener(mClusterManager);
        addItems();
    }

    private void addItems() {
        String dateKontrol = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(System.currentTimeMillis());
        long k=1;
        Cursor user = database.query(DBHelper.TABLE,
                new String[]{DBHelper.KEY_ID, DBHelper.KEY_DATE, DBHelper.KEY_LAT, DBHelper.KEY_LON},
                 DBHelper.KEY_DATE+"=?", new String[]{dateKontrol}, null, null,
                DBHelper.KEY_DATE + " DESC");

        while (user.moveToNext()) {
            if((k%2)==1) {
                Double lat = user.getDouble(user.getColumnIndex(DBHelper.KEY_LAT));
                Double lon = user.getDouble(user.getColumnIndex(DBHelper.KEY_LON));

                MyItem offsetItem = new MyItem(lat, lon);
                mClusterManager.addItem(offsetItem);
            }
            k=k+1;
        }
        user.close();
    }

    private void addMarker(long k, PolylineOptions line) {
        Cursor zero = database.query(DBHelper.TABLE, new String[] { DBHelper.KEY_ID }, DBHelper.KEY_ID + "=?",
                new String[] { String.valueOf(k) }, null, null, null, null);
        if(zero.moveToFirst()) {

            Double lat = zero.getDouble(zero.getColumnIndex(DBHelper.KEY_LAT));
            Double lon = zero.getDouble(zero.getColumnIndex(DBHelper.KEY_LON));
            mClusterManager = new ClusterManager<>(this, googleMap);
            googleMap.setOnCameraChangeListener(mClusterManager);
            googleMap.setOnMarkerClickListener(mClusterManager);
            MyItem offsetItem = new MyItem(lat, lon);
            mClusterManager.addItem(offsetItem);
            LatLng latLng = new LatLng(lat, lon);
            line.add(latLng);
            line.width(8f).color(Color.GREEN);
        }
        zero.close();
    }




    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.Information:
                if (dbHelper.countBD() != 0)
                    showDialog(1);
                else
                    show();
                break;
            case R.id.Setting:
                Intent intent = new Intent(this, Setting.class);
                startActivity(intent);
                break;
        }
    }


    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case 1:
                final ViewGroup nullParent = null;
                View view = LayoutInflater.from(this).inflate(R.layout.todate, nullParent);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setView(view);

                final EditText userFirstDate = (EditText) view.findViewById(R.id.input_text);
                userFirstDate.setInputType(InputType.TYPE_NULL);
                final EditText userSecondDate = (EditText) view.findViewById(R.id.input_text1);
                userSecondDate.setInputType(InputType.TYPE_NULL);
                final DatePickerDialog pickerDialogFirst;
                final DatePickerDialog pickerDialogSecond;

                userFirstDate.setText(null);
                userSecondDate.setText(null);

                Calendar newCalendar=Calendar.getInstance();
                final SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                pickerDialogFirst=new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                    // функция onDateSet обрабатывает шаг 2: отображает выбранные нами данные в элементе EditText
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        Calendar newCal=Calendar.getInstance();
                        newCal.set(year,monthOfYear,dayOfMonth);
                        userFirstDate.setText(dateFormat.format(newCal.getTime()));
                    }
                },newCalendar.get(Calendar.YEAR),newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
                pickerDialogFirst.setTitle(R.string.titlefrom);
                pickerDialogSecond=new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                    // функция onDateSet обрабатывает шаг 2: отображает выбранные нами данные в элементе EditText
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        Calendar newCal=Calendar.getInstance();
                        newCal.set(year,monthOfYear,dayOfMonth);
                        userSecondDate.setText(dateFormat.format(newCal.getTime()));
                    }
                },newCalendar.get(Calendar.YEAR),newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
                pickerDialogSecond.setTitle(R.string.titlebefore);


                userFirstDate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (hasFocus) {
                            pickerDialogFirst.show();
                        }
                    }
                });

                userSecondDate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (hasFocus) {
                            pickerDialogSecond.show();
                        }
                    }
                });

                builder
                        .setCancelable(false)
                        .setPositiveButton(R.string.show,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {

                                        String l1 = userFirstDate.getText().toString();
                                        String l2 = userSecondDate.getText().toString();

                                        if (l1.isEmpty() || l2.isEmpty()) {
                                            AlertDialog.Builder quitDialog = new AlertDialog.Builder(MapsActivity.this);
                                            quitDialog.setMessage("Заполните все поля!");
                                            quitDialog.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.cancel();
                                                }
                                            });
                                            quitDialog.show();

                                        } else {

                                            try {

                                                Date date1 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(l1);
                                                Date date2 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(l2);

                                                if(Math.ceil((date2.getTime() - date1.getTime()) / 1000 / 60 / 60 / 24) >= 0){

                                                    toDate(l1,l2);
                                                    userFirstDate.setText(null);
                                                    userSecondDate.setText(null);

                                                }
                                                else{
                                                    AlertDialog.Builder quitDialog = new AlertDialog.Builder(MapsActivity.this);
                                                    quitDialog.setMessage("Некоректно задан диапозон " + ": " + "измените введенные значения.");
                                                    quitDialog.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            userFirstDate.setText(null);
                                                            userSecondDate.setText(null);
                                                            dialog.cancel();
                                                        }
                                                    });
                                                    quitDialog.show();
                                                }

                                            } catch (ParseException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();

                                    }
                                });

                return builder.create();

            default:
                return null;
        }
    }


    /**
     * Unload by date
     * @param date1 first parameter
     * @param date2 second parameter
     */
    private void toDate(String date1, String date2) {
        database = dbHelper.getWritableDatabase();
        PolylineOptions line = new PolylineOptions();

        Cursor zero = database.rawQuery("select * from " + DBHelper.TABLE + " where " + DBHelper.KEY_DATE + " BETWEEN '" +
                date1 + "' AND '" + date2 + "'" + " ORDER BY " + DBHelper.KEY_DATE + " DESC, "+DBHelper.KEY_TIME+" DESC", null);

        if (zero.moveToFirst()) {
            do {
                long id = zero.getLong(zero.getColumnIndex(DBHelper.KEY_ID));
                addMarker(id, line);
                Double lat = zero.getDouble(zero.getColumnIndex(DBHelper.KEY_LAT));
                Double lon = zero.getDouble(zero.getColumnIndex(DBHelper.KEY_LON));
                mClusterManager = new ClusterManager<>(this, googleMap);
                googleMap.setOnCameraChangeListener(mClusterManager);
                googleMap.setOnMarkerClickListener(mClusterManager);
                MyItem offsetItem = new MyItem(lat, lon);
                mClusterManager.addItem(offsetItem);
                LatLng latLng = new LatLng(lat, lon);
                line.add(latLng);
                line.width(8f).color(Color.GREEN);
            } while (zero.moveToNext());
        }
        zero.close();
        googleMap.addPolyline(line);
    }

    /**
     * Dialogue, whether the base is empty
     */
    private void show(){
        AlertDialog.Builder quitDialog = new AlertDialog.Builder(MapsActivity.this);
        quitDialog.setMessage("Вы еще не ходили по маршрутам!");
        quitDialog.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
            }
        });
        quitDialog.show();
    }
}


