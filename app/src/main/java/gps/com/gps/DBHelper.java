package gps.com.gps;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;

/**
 * Класс, отвечающий за работу с БД
 */
public class DBHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1; //Версия БД
    public static final String DATABASE_NAME = "dateDb.db"; //Название БД
    public static final String TABLE = "datebase"; //Имя

    //Поля в БД
    public static final String KEY_ID = "_id";
    public static final String KEY_LAT = "latitude";
    public static final String KEY_LON = "longitude";
    public static final String KEY_ACC = "accuracy";
    public static final String KEY_PROV = "provider";
    public static final String KEY_BAT = "battery";
    public static final String KEY_DATE = "date";
    public static final String KEY_TIME = "time";


    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    /**
     * Создание БД
     * @param db
     */
    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("create table " + TABLE + "(" + KEY_ID
                + " integer primary key," + KEY_LAT + " real," + KEY_LON + " real," + KEY_ACC+ " real,"+KEY_PROV+ " text,"+
                KEY_BAT+ " text,"+ KEY_DATE+" text,"+KEY_TIME + " text"+");");



    }

    /**
     * Пересоздание БД с новой версией
     * @param db
     * @param oldVersion
     * @param newVersion
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + TABLE);

        onCreate(db);

    }
    /**
     * Удаление
     */
    public void deleteTable() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE, null, null);
        db.close();
    }


    /*
     * Описываем структуру данных для добавления в таблицу всех пармаметров
     */
    protected ContentValues createContentValues(double lat, double lon, double acc,String prov, String bat) {
        String DATE_KONTROL = new SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis());

        String TIME_KONTROL = new SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis());
        ContentValues values = new ContentValues();
        values.put(DBHelper.KEY_LAT, lat);
        values.put(DBHelper.KEY_LON, lon);
        values.put(DBHelper.KEY_ACC, acc);
        values.put(DBHelper.KEY_PROV, prov);
        values.put(DBHelper.KEY_BAT, bat);
        values.put(KEY_DATE, DATE_KONTROL);
        values.put(KEY_TIME, TIME_KONTROL);
        return values;
    }

    /**
     * Создаём новый элемент в базе.
     */
    public void createNewTable(double lat, double lon, double acc,String prov, String bat) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues initialValues = createContentValues(lat, lon, acc, prov, bat);
        db.insert(TABLE, null, initialValues);
        db.close();
    }

    public long countBD() {
        SQLiteDatabase db = this.getReadableDatabase();
        //получаем данные из ДБ
        return db.rawQuery("SELECT * FROM " + DBHelper.TABLE, null).getCount();
    }



}
