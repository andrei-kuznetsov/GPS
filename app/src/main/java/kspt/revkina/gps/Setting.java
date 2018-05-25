package kspt.revkina.gps;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.view.View;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


/**
 * Class, settings for working with the database
 */
public class Setting extends PreferenceActivity implements View.OnClickListener {
    SharedPreferences sharedPreferences;
    String nameFile;
    private LocationService locationService = new LocationService();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                new Thread(
                        new Runnable() {
                            public void run() {
                                try {
                                    exportTheDB();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                ).start();
                break;
            case R.id.buttonClear:
                clearDialog();
                break;
            case R.id.enter:
                SwitchPreference enter = (SwitchPreference) findPreference("enter");
                if (enter.getSwitchTextOn().equals(R.string.summaryOn)) {
                    locationService.updateLocationRequest(
                            Long.parseLong(sharedPreferences.getString(getString(R.string.seconds), "120")),
                            Float.parseFloat(sharedPreferences.getString(getString(R.string.meters), "10")));
                } else {
                    startService(new Intent(this, LocationService.class));
                }
                break;
        }
    }

    private Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            String fileName = bundle.getString("fileName");
            AlertDialog.Builder quitDialog = new AlertDialog.Builder(Setting.this);
            quitDialog.setTitle(R.string.titleExportBD);
            quitDialog.setMessage(fileName);
            quitDialog.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            quitDialog.show();
        }
    };

    private void clearDialog() {
        new AlertDialog.Builder(Setting.this)
            .setMessage(R.string.messageClearBD)
            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
              }
        })
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new Thread(
                        new Runnable() {
                            public void run() {
                                DBHelper dbHelper = new DBHelper(getApplicationContext());
                                dbHelper.deleteTable();
                            }
                        }
                ).start();
            }
        }).show();
    }

    private void exportTheDB() throws IOException {
        Message msg = handler.obtainMessage();
        File file;
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss", Locale.getDefault());
        String timeStampDB = sdf.format(Calendar.getInstance().getTime());
        DBHelper dbHelper = new DBHelper(getApplicationContext());
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        String path = Environment.getExternalStorageDirectory().getPath() + "/GPS_Tracker";

        File filePath = new File(path);
        if (!filePath.exists()) {
            if (filePath.mkdirs())
                System.out.print("Add catalog for export");
        }

        file = new File(path + "/" + timeStampDB + ".csv");

        if (file.exists())
            if (file.delete())
                System.out.print("Delete repeat catalog");

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(path + "/" + timeStampDB + ".csv", true), "Windows-1251"));


        Cursor cursor = database.query(DBHelper.TABLE, null, null, null, null, null, DBHelper.KEY_DATE + " DESC");
        writer.println("_ID"+ ";" +"Date"+ ";"+"Latitude"+ ";"+"Longitude" + ";"+"Accuracy"+ ";"+"Provider"+
                ";"+"BatteryLife"+";"+"Difference");

        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndex(DBHelper.KEY_ID));
                String idSecond = "";
                long different = 0;
                Long dataD = cursor.getLong(cursor.getColumnIndex(DBHelper.KEY_DATE));
                if (Long.parseLong(id) < dbHelper.countBD()) {
                    long second = Long.parseLong(id)+1;
                    idSecond = String.valueOf(second);
                    Cursor cursor2 = database.rawQuery("select * from " + DBHelper.TABLE + " where " + idSecond + " = " +
                            DBHelper.KEY_ID, null);
                    cursor2.moveToFirst();
                    long dataSecond =  cursor2.getLong(cursor2.getColumnIndex(DBHelper.KEY_DATE));
                    different = (dataSecond - dataD) / 1000L;
                }

                Date netDate = (new Date(dataD));
                String data = sdf.format(netDate);
                String lat = cursor.getString(cursor.getColumnIndex(DBHelper.KEY_LAT));
                String lon = cursor.getString(cursor.getColumnIndex(DBHelper.KEY_LON));
                String acc = cursor.getString(cursor.getColumnIndex(DBHelper.KEY_ACC));
                String bat = cursor.getString(cursor.getColumnIndex(DBHelper.KEY_BAT));
                String prov = cursor.getString(cursor.getColumnIndex(DBHelper.KEY_PROV));

                writer.println(id + ";" + data + ";" + lat + ";" + lon + ";" + acc+ ";"+ prov+ ";" + bat +";"+ different);
            } while (cursor.moveToNext());
        }

        cursor.close();
        writer.flush();
        writer.close();
        MediaScannerConnection.scanFile(this, new String[]{path+"/"+timeStampDB+".csv"}, null, null);
        nameFile = path+"/"+timeStampDB+".csv";
        database.close();
        Bundle bundle = new Bundle();
        bundle.putString("fileName", path+"/"+timeStampDB+".csv");
        msg.setData(bundle);
        handler.sendMessage(msg);
    }
}