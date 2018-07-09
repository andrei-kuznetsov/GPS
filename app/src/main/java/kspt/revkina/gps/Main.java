package kspt.revkina.gps;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

/**
 * Input screen for receiving settings
 */
public class Main extends AppCompatActivity {

    public static final int REQUEST_CODE = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WAKE_LOCK},
                REQUEST_CODE);

        setContentView(R.layout.first_start);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED&&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
            Intent intent=new Intent(this, MapsActivity.class);
            startActivity(intent);
            finish();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.titlePermission)
                    .setMessage(R.string.messagePermission)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(Main.this, new String[]{
                                    android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WAKE_LOCK}, 4);
                        }
                    })
                    .create()
                    .show();
        }
    }
}