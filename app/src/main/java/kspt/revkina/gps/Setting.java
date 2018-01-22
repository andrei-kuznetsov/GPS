package kspt.revkina.gps;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.EditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Class, settings for working with the database
 */
public class Setting extends Activity implements OnClickListener{

    private DBHelper dbHelper;
    private static final String APP_PREFERENCES = "settings";
    private static final String APP_PREFERENCES_METRES = "metres";
    private static final String APP_PREFERENCES_TIME = "time";
    private SharedPreferences sharedPrefs;
    private SharedPreferences.Editor editor;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);
        sharedPrefs = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        Button btnSave = (Button) findViewById(R.id.btnSave);
        btnSave.setOnClickListener(this);

        Button btnClear = (Button) findViewById(R.id.btnClear);
        btnClear.setOnClickListener(this);
        Button btnSettings = (Button) findViewById(R.id.btnSettings);
        btnSettings.setOnClickListener(this);

        dbHelper = new DBHelper(getApplicationContext());

    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
                Intent intent = new Intent(this, MapsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSettings:
                showDialog(0);
                this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                break;
            case R.id.btnSave:
                    try {
                        exportTheDB();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                break;
            case R.id.btnClear:
                AlertDialog.Builder quitDialog = new AlertDialog.Builder(Setting.this);
                quitDialog.setMessage("Вы действительно хотите удалить все данные?");
                quitDialog.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                quitDialog.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbHelper.deleteTable();
                    }
                });
                quitDialog.show();
                break;
        }

    }


    private void exportTheDB() throws IOException {

        File file;
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault());
        String timeStampDB = sdf.format(cal.getTime());
        SimpleDateFormat times = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String Time = times.format(cal.getTime());
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        String path = Environment.getExternalStorageDirectory().getPath() + "/GPS_Tracker";
        timeStampDB=timeStampDB+"_"+Time;

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


        Cursor zero = database.query(DBHelper.TABLE,
                null, null, null,
                null, null, DBHelper.KEY_DATE + " DESC, " + DBHelper.KEY_TIME + " DESC");
        writer.println("_ID"+ ";" +"Date"+ ";"+"Time"+ ";"+"Latitude"+ ";"+"Longitude" + ";"+"Accuracy"+ ";"+"Provider"+
                ";"+"BatteryLife");

        if (zero.moveToFirst()) {
            do {
                String id = zero.getString(zero.getColumnIndex(DBHelper.KEY_ID));
                String data = zero.getString(zero.getColumnIndex(DBHelper.KEY_DATE));
                String time = zero.getString(zero.getColumnIndex(DBHelper.KEY_TIME));
                String lat = zero.getString(zero.getColumnIndex(DBHelper.KEY_LAT));
                String lon = zero.getString(zero.getColumnIndex(DBHelper.KEY_LON));
                String acc = zero.getString(zero.getColumnIndex(DBHelper.KEY_ACC));
                String bat = zero.getString(zero.getColumnIndex(DBHelper.KEY_BAT));
                String prov = zero.getString(zero.getColumnIndex(DBHelper.KEY_PROV));

                writer.println(id + ";" + data + ";" + time + ";" + lat + ";" + lon + ";" + acc+ ";"+ prov+ ";" + bat);
            } while (zero.moveToNext());
        }

        zero.close();
        writer.flush();
        writer.close();
        MediaScannerConnection.scanFile(this, new String[]{path+"/"+timeStampDB+".csv"}, null, null);
        AlertDialog.Builder quitDialog = new AlertDialog.Builder(Setting.this);
        quitDialog.setMessage("Вывели файл:\n "+path+"/"+timeStampDB+".csv");
        quitDialog.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        quitDialog.show();
        database.close();
    }


    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case 0:
                final ViewGroup nullParent = null;
                View view = LayoutInflater.from(this).inflate(R.layout.accuracy, nullParent);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setView(view);
                int metre = 10;
                int second = 120;
                if(sharedPrefs.contains(APP_PREFERENCES_METRES)){
                    metre = sharedPrefs.getInt(APP_PREFERENCES_METRES, 10);
                }
                if(sharedPrefs.contains(APP_PREFERENCES_TIME)){
                    second = sharedPrefs.getInt(APP_PREFERENCES_TIME, 120);
                }
                final EditText metres = (EditText) view.findViewById(R.id.etMetres);
                final EditText seconds = (EditText) view.findViewById(R.id.etSeconds);
                metres.setInputType(InputType.TYPE_CLASS_NUMBER);
                seconds.setInputType(InputType.TYPE_CLASS_NUMBER);
                metres.setSelectAllOnFocus(true);
                seconds.setSelectAllOnFocus(true);
                metres.setText(String.valueOf(metre));
                seconds.setText(String.valueOf(second));
                builder.setTitle("Точность")
                        .setCancelable(false)
                        .setPositiveButton(R.string.change,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        String met =metres.getText().toString();
                                        String sec =seconds.getText().toString();

                                        if (met.isEmpty()||sec.isEmpty()) {
                                            AlertDialog.Builder quitDialog = new AlertDialog.Builder(Setting.this);
                                            quitDialog.setMessage("Вы не заполнили все поля настроек. Новые настройки не сохранены.");
                                            quitDialog.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.cancel();
                                                }
                                            });
                                            quitDialog.show();
                                        } else {
                                            editor = sharedPrefs.edit();
                                            editor.putInt(APP_PREFERENCES_METRES, Integer.valueOf(met));
                                            editor.putInt(APP_PREFERENCES_TIME, Integer.valueOf(sec));
                                            editor.apply();
                                        }

                                    }
                                })

                        .setNegativeButton(android.R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        dialog.cancel();
                                    }
                                }).create().show();
                return null;
            default:
                return null;
        }
    }
}
