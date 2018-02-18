package kspt.revkina.gps;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
