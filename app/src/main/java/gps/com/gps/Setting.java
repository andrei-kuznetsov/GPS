package gps.com.gps;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.view.View.OnClickListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Ира on 23.06.2017.
 */

public class Setting extends Activity implements OnClickListener{

    Button btnSave, btnClear; //Кнопки для выгрузки и преходов
    SQLiteDatabase database; //БД
    DBHelper dbHelper;//Создание БД

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting);

        btnSave = (Button) findViewById(R.id.btnSave);
        btnSave.setOnClickListener(this);

        btnClear = (Button) findViewById(R.id.btnClear);
        btnClear.setOnClickListener(this);

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

            case R.id.btnSave: //Переход к выгрузке
                    try {
                        exportTheDB();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                break;
            case R.id.btnClear: //Очистить данные
                AlertDialog.Builder quitDialog = new AlertDialog.Builder(Setting.this);
                quitDialog.setMessage("Вы действительно хотите удалить все данные?");
                quitDialog.setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                quitDialog.setPositiveButton("Да", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dbHelper.deleteTable();
                    }
                });
                quitDialog.show();
                break;
        }

    }




    /**
     * Выгрузка всех данных
     * @throws IOException
     */
    private void exportTheDB() throws IOException {

        File file;
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");
        String TimeStampDB = sdf.format(cal.getTime());
        database = dbHelper.getWritableDatabase();

        // проверяем, есть ли папка по указанному пути. если нет, то создаем. getExternalStoragePublicDirectory
        String path = Environment.getExternalStorageDirectory().getPath() + "/GPS_Tracker";


        File filePath = new File(path);
        if (!filePath.exists()) { //есть ли папка, если нет, то создаем
            filePath.mkdirs();
        }

        file = new File(path + "/" + TimeStampDB + ".csv");


        if (file.exists()) file.delete(); //создаем файл

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(path + "/" + TimeStampDB + ".csv", true), "Windows-1251"));
        //Cursor zero = database.rawQuery("SELECT * FROM " + DBHelper.TABLE, null);

        Cursor zero = database.query(DBHelper.TABLE,
                new String[]{DBHelper.KEY_ID, DBHelper.KEY_DATE, DBHelper.KEY_TIME, DBHelper.KEY_LAT, DBHelper.KEY_LON},
                null, null,
                null, null, DBHelper.KEY_DATE + " DESC, " + DBHelper.KEY_TIME + " DESC");
        writer.println("_ID"+ ";" +"Дата"+ ";"+"Время"+ ";"+"Долгота"+ ";"+"Широта");

        if (zero.moveToFirst()) {
            do {
                String id = zero.getString(zero.getColumnIndex(DBHelper.KEY_ID));
                String data = zero.getString(zero.getColumnIndex(DBHelper.KEY_DATE));
                String time = zero.getString(zero.getColumnIndex(DBHelper.KEY_TIME));
                String lat = zero.getString(zero.getColumnIndex(DBHelper.KEY_LAT));
                String lon = zero.getString(zero.getColumnIndex(DBHelper.KEY_LON));

                writer.println(id + ";" + data + ";" + time + ";" + lat + ";" + lon);
            } while (zero.moveToNext());
        }

        zero.close();
        writer.flush();
        writer.close();
        MediaScannerConnection.scanFile(this, new String[]{path+"/"+TimeStampDB+".csv"}, null, null);
        AlertDialog.Builder quitDialog = new AlertDialog.Builder(Setting.this);
        quitDialog.setMessage("Вывели файл:\n "+path+"/"+TimeStampDB+".csv");
        quitDialog.setNegativeButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        quitDialog.show();
        database.close();
    }
}
