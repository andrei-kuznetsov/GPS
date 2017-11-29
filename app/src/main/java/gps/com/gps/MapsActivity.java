package gps.com.gps;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.clustering.ClusterManager;

import android.view.View.OnClickListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


/**
 * Интерфейс приложения
 */
public class MapsActivity extends FragmentActivity implements OnClickListener{

    GoogleMap googleMap;
    SQLiteDatabase database; //БД
    DBHelper dbHelper;//Создание БД
    private final int IDD_CHECK_DATE = 1;
    ImageButton img;
    // Declare a variable for the cluster manager.
    private ClusterManager<MyItem> mClusterManager;
    public static final String APP_PREFERENCES = "settings";
    /**
     * Название параметра настроек
     */
    public static final String APP_PREFERENCES_METRES = "metres";
    /**
     * Название параметра настроек
     */
    public static final String APP_PREFERENCES_TIME = "time";
    /**
     *Хранилище, используемое приложениями для хранения своих настроек
     */
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
        googleMap = fm.getMap();
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.setMyLocationEnabled(true);
        database = dbHelper.getWritableDatabase();

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

    /**
     *
     */
    private void startTracker() {
        if (!isGooglePlayServicesAvailable()) {
            return;
        }
        Intent gpsTrackerIntent = new Intent(getBaseContext(), GPSTracker.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getBaseContext(), 0, gpsTrackerIntent, 0);
        //используется для отправки пользователю разовых или повторяющихся сообщений в заданное время
        AlarmManager alarmManager = (AlarmManager) getBaseContext().getSystemService(Context.ALARM_SERVICE);
        //задаёт повторяющиеся сигнализации с фиксированным временным интервалом
        int seconds = 120;
        if(sharedPrefs.contains(APP_PREFERENCES_TIME)){
            seconds = sharedPrefs.getInt(APP_PREFERENCES_TIME, 120);
        }
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                //о прошествии указанного промежутка времени с момента загрузки выводит устройство
                // из спящего режима и запускает ожидающее намерение. Используется системное время
                SystemClock.elapsedRealtime(),
                1*10000*seconds, // 60000 = 2 minute
                pendingIntent); //объект PendingIntent, определяющий действие, выполняемое при запуске сигнализации

        if (gpsTrackerIntent!=null&&dbHelper.countBD()!=0)
            setUpClusterer();

    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }



    private void setUpClusterer() {
        Cursor zero = database.rawQuery("select * from " + DBHelper.TABLE + " where " + DBHelper.KEY_ID + "=" +
                dbHelper.countBD(), null);
        zero.moveToFirst();
        String lats = zero.getString(zero.getColumnIndex(DBHelper.KEY_LAT));
        String lons = zero.getString(zero.getColumnIndex(DBHelper.KEY_LON));

        double latit = Double.parseDouble(lats);
        double longit = Double.parseDouble(lons);
        // Position the map.
        int metres = 10;
        if(sharedPrefs.contains(APP_PREFERENCES_METRES)){
            metres = sharedPrefs.getInt(APP_PREFERENCES_METRES, 10);
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latit, longit), metres));


        // Initialize the manager with the context and the map.
        // (Activity extends context, so we can pass 'this' in the constructor.)
        mClusterManager = new ClusterManager<MyItem>(this, googleMap);

        // Point the map's listeners at the listeners implemented by the cluster
        // manager.
        googleMap.setOnCameraChangeListener(mClusterManager);
        googleMap.setOnMarkerClickListener(mClusterManager);

        // Add cluster items (markers) to the cluster manager.
        addItems();
    }

    private void addItems() {
        String DATE_KONTROL = new SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis());
        // Add ten cluster items in close proximity, for purposes of this example.
        long k=1;
        Cursor user = database.query(DBHelper.TABLE,
                new String[]{DBHelper.KEY_ID, DBHelper.KEY_DATE, DBHelper.KEY_LAT, DBHelper.KEY_LON},
                 DBHelper.KEY_DATE+"=?", new String[]{DATE_KONTROL}, null, null,
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
    }

    private void addMarker(long k, PolylineOptions line) {
        Cursor zero = database.query(DBHelper.TABLE, new String[] { DBHelper.KEY_ID }, DBHelper.KEY_ID + "=?",
                new String[] { String.valueOf(k) }, null, null, null, null);

//        if (cursor != null){
//            cursor.moveToFirst();
//        }
//        Cursor zero = database.rawQuery("select * from " + DBHelper.TABLE + " where " + DBHelper.KEY_ID + "=" + k
//                , null);
        if(zero.moveToFirst()) {

            Double lat = zero.getDouble(zero.getColumnIndex(DBHelper.KEY_LAT));
            Double lon = zero.getDouble(zero.getColumnIndex(DBHelper.KEY_LON));
            mClusterManager = new ClusterManager<MyItem>(this, googleMap);

            // Point the map's listeners at the listeners implemented by the cluster manager.
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
                //Получаем вид с файла todate.xml, который применим для диалогового окна:
                LayoutInflater lid = LayoutInflater.from(this);
                View promptsViewd = lid.inflate(R.layout.todate, null);
                //Создаем AlertDialog
                AlertDialog.Builder mbuilderd = new AlertDialog.Builder(this);

                //Настраиваем prompt.xml для нашего AlertDialog:
                mbuilderd.setView(promptsViewd);

                //Настраиваем отображение поля для ввода текста в открытом диалоге:
                final EditText userInputd = (EditText) promptsViewd.findViewById(R.id.input_text);
                userInputd.setInputType(InputType.TYPE_NULL);
                final EditText userInput1d = (EditText) promptsViewd.findViewById(R.id.input_text1);
                userInput1d.setInputType(InputType.TYPE_NULL);
                final DatePickerDialog dateBirdayDatePicker;
                final DatePickerDialog dateBirdayDatePicker1;
                //эта функция делает шаг 1: создает объект DatePickerDialog
                userInputd.setText(null);
                userInput1d.setText(null);

                Calendar newCalendar=Calendar.getInstance(); // объект типа Calendar мы будем использовать для получения даты
                final SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd"); // это строка нужна для дальнейшего преобразования даты в строку
                //создаем объект типа DatePickerDialog и инициализируем его конструктор обработчиком события выбора даты и данными для даты по умолчанию
                dateBirdayDatePicker=new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                    // функция onDateSet обрабатывает шаг 2: отображает выбранные нами данные в элементе EditText
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        Calendar newCal=Calendar.getInstance();
                        newCal.set(year,monthOfYear,dayOfMonth);
                        userInputd.setText(dateFormat.format(newCal.getTime()));
                    }
                },newCalendar.get(Calendar.YEAR),newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
                dateBirdayDatePicker.setTitle("От:");
                dateBirdayDatePicker1=new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
                    // функция onDateSet обрабатывает шаг 2: отображает выбранные нами данные в элементе EditText
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        Calendar newCal=Calendar.getInstance();
                        newCal.set(year,monthOfYear,dayOfMonth);
                        userInput1d.setText(dateFormat.format(newCal.getTime()));
                    }
                },newCalendar.get(Calendar.YEAR),newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
                dateBirdayDatePicker1.setTitle("До:");


                userInputd.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (hasFocus) {
                            dateBirdayDatePicker.show();
                        }
                    }
                });

                userInput1d.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (hasFocus) {
                            dateBirdayDatePicker1.show();
                        }
                    }
                });


                //Настраиваем сообщение в диалоговом окне:
                mbuilderd
                        .setCancelable(false)
                        .setPositiveButton("Показать",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {

                                        //Вводим текст и отображаем в строке ввода на основном экране:
                                        String l1 = userInputd.getText().toString();
                                        String l2 = userInput1d.getText().toString();

                                        if (l1.isEmpty() || l2.isEmpty()) {
                                            AlertDialog.Builder quitDialog = new AlertDialog.Builder(MapsActivity.this);
                                            quitDialog.setMessage("Заполните все поля!");
                                            quitDialog.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.cancel();
                                                }
                                            });
                                            quitDialog.show();
                                            return;

                                        } else {

                                            try {

                                                Date date1 = new SimpleDateFormat("yyyy-MM-dd").parse(l1);
                                                Date date2 = new SimpleDateFormat("yyyy-MM-dd").parse(l2);

                                                //Сравнение данных, правильно заданы ли даты
                                                if(Math.ceil((date2.getTime() - date1.getTime()) / 1000 / 60 / 60 / 24) >= 0){

                                                    toDate(l1,l2);
                                                    userInputd.setText(null);
                                                    userInput1d.setText(null);

                                                }
                                                else{
                                                    AlertDialog.Builder quitDialog = new AlertDialog.Builder(MapsActivity.this);
                                                    quitDialog.setMessage("Некоректно задан диапозон " + ": " + "измените введенные значения.");
                                                    quitDialog.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            userInputd.setText(null);
                                                            userInput1d.setText(null);
                                                            dialog.cancel();
                                                        }
                                                    });
                                                    quitDialog.show();
                                                }

                                            } catch (ParseException e) {
                                                e.printStackTrace();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                })
                        .setNegativeButton("Отмена",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();

                                    }
                                });

                return mbuilderd.create();

            default:
                return null;
        }
    }

    /**
     * Выгрузка по дате
     * @throws IOException
     */
    private void toDate(String date1, String date2) throws IOException {
        database = dbHelper.getWritableDatabase();
        PolylineOptions line = new PolylineOptions();

        Cursor zero = database.rawQuery("select * from " + DBHelper.TABLE + " where " + DBHelper.KEY_DATE + " BETWEEN '" +
                date1 + "' AND '" + date2 + "'" + " ORDER BY " + DBHelper.KEY_DATE + " DESC, "+DBHelper.KEY_TIME+" DESC", null);

        if (zero.moveToFirst()) {
            do {
                long id = zero.getLong(zero.getColumnIndex(DBHelper.KEY_ID));
               // addMarker(id, line);
                Double lat = zero.getDouble(zero.getColumnIndex(DBHelper.KEY_LAT));
                Double lon = zero.getDouble(zero.getColumnIndex(DBHelper.KEY_LON));
                mClusterManager = new ClusterManager<MyItem>(this, googleMap);

                // Point the map's listeners at the listeners implemented by the cluster manager.
                googleMap.setOnCameraChangeListener(mClusterManager);
                googleMap.setOnMarkerClickListener(mClusterManager);
                MyItem offsetItem = new MyItem(lat, lon);
                mClusterManager.addItem(offsetItem);
                LatLng latLng = new LatLng(lat, lon);
                line.add(latLng);
                line.width(8f).color(Color.GREEN);
            } while (zero.moveToNext());
        }

        googleMap.addPolyline(line);
    }

    /**
     * Диалог, пустая ли база
     */
    private void show(){
        AlertDialog.Builder quitDialog = new AlertDialog.Builder(MapsActivity.this);
        quitDialog.setMessage("Вы еще не ходили по маршрутам!");
        quitDialog.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
            }
        });
        quitDialog.show();
    }

}


