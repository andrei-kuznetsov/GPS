package kspt.revkina.gps;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;
import android.view.View.OnClickListener;

/**
 * Input screen for receiving settings
 */
public class Main extends Activity implements OnClickListener{

    private static final String APP_PREFERENCES = "settings";
    private static final String APP_PREFERENCES_METRES = "metres";
    private static final String APP_PREFERENCES_TIME = "time";
    private SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPrefs = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        ActivityCompat.requestPermissions(this, new String[]{
                android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.ACCESS_COARSE_LOCATION}, 3);

        setContentView(R.layout.first_start);

        ImageView ImageView= (ImageView) findViewById(R.id.ImageView);
        ImageView.setImageResource(R.drawable.gps);
        getPermission();

    }
    private void getPermission(){
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED&&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
            if(!sharedPrefs.contains(APP_PREFERENCES_METRES)&& !sharedPrefs.contains(APP_PREFERENCES_TIME)) {
                SharedPreferences.Editor editor;
                editor = sharedPrefs.edit();
                editor.putInt(APP_PREFERENCES_METRES, 10);
                editor.putInt(APP_PREFERENCES_TIME, 120);
                editor.apply();
            }
            Intent intent=new Intent(this, MapsActivity.class);
            startActivity(intent);
            finish();
        }
    }


    @Override
    public void onClick(View v) {
        getPermission();
    }
}
