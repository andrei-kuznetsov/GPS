package kspt.revkina.gps;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * The class responsible for working with the database
 */
class DBHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "dateDb.db";
    public static final String TABLE = "gps";
    public static final String KEY_ID = "_id";
    public static final String KEY_LAT = "latitude";
    public static final String KEY_LON = "longitude";
    public static final String KEY_ACC = "accuracy";
    public static final String KEY_PROV = "provider";
    public static final String KEY_BAT = "battery";
    public static final String KEY_DATE = "date";


    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL("create table " + TABLE + "(" + KEY_ID
                + " integer primary key," + KEY_LAT + " real," + KEY_LON + " real," + KEY_ACC+ " real,"+KEY_PROV+ " text,"+
                KEY_BAT+ " text,"+ KEY_DATE+" real"+");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + TABLE);
        onCreate(db);
    }

    public void deleteTable() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE, null, null);
        db.close();
    }

    private ContentValues createContentValues(double lat, double lon, double acc, long time, String prov, String bat) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.KEY_LAT, lat);
        values.put(DBHelper.KEY_LON, lon);
        values.put(DBHelper.KEY_ACC, acc);
        values.put(DBHelper.KEY_PROV, prov);
        values.put(DBHelper.KEY_BAT, bat);
        values.put(KEY_DATE, time);
        return values;
    }

    public void createNewNote(double lat, double lon, double acc, long time, String prov, String bat) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues initialValues = createContentValues(lat, lon, acc, time, prov, bat);
        db.insert(TABLE, null, initialValues);
        db.close();
    }

    public long countBD() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + DBHelper.TABLE, null).getCount();
    }
}