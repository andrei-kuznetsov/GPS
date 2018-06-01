package kspt.revkina.gps;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class SpeedListActivity extends AppCompatActivity  {

    SimpleAdapter adapter;
    ArrayList<HashMap<String, String>> mList = new ArrayList<>();
    SharedPreferences pref;
    private ConversionListGson conversionListGson = new ConversionListGson();
    private static final String KEY_SPEED = "speed";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_option);
        ListView userList = findViewById(R.id.userList);

        pref = getApplicationContext().getSharedPreferences("list", 0);
        String storedCollection = pref.getString(KEY_SPEED, null);
        if (storedCollection != null) {
            mList = conversionListGson.getSpeedList(KEY_SPEED, pref, mList);
        }

        LinearLayout layoutList = new LinearLayout(this);
        layoutList.setOrientation(LinearLayout.VERTICAL);
        final TextView speedTextView = new TextView(this);
        final TextView parTextView = new TextView(this);
        layoutList.addView(speedTextView);
        layoutList.addView(parTextView);

        adapter = new SimpleAdapter(this, mList, android.R.layout.simple_list_item_2,
                new String[]{"speed","par"},
                new int[]{android.R.id.text1, android.R.id.text2});
        userList.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog(0);
            }
        });
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case 0:
                LinearLayout layout = new LinearLayout(this);
                layout.setOrientation(LinearLayout.VERTICAL);
                final EditText speedFromEditText = new EditText(this);
                final EditText speedBeforeEditText = new EditText(this);
                final EditText secondsEditText = new EditText(this);
                final EditText metersEditText = new EditText(this);

                speedFromEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
                speedBeforeEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
                secondsEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
                metersEditText.setInputType(InputType.TYPE_CLASS_NUMBER);

                speedFromEditText.setHint(R.string.from);
                speedBeforeEditText.setHint(R.string.before);
                secondsEditText.setHint(R.string.seconds);
                metersEditText.setHint(R.string.meters);
                layout.addView(speedFromEditText);
                layout.addView(speedBeforeEditText);
                layout.addView(secondsEditText);
                layout.addView(metersEditText);

                new AlertDialog.Builder(this)
                        .setTitle("Введите диапозон скоростей - м/c:")
                        .setView(layout)
                        .setPositiveButton("Добавить", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String fromSpeed = String.valueOf(speedFromEditText.getText());
                                String beforeSpeed = String.valueOf(speedBeforeEditText.getText());
                                String seconds = String.valueOf(secondsEditText.getText());
                                String meters = String.valueOf(metersEditText.getText());
                                HashMap<String, String> hm = new HashMap<>();
                                hm.put("speed", "Speed: "+fromSpeed+ " - "+beforeSpeed);
                                hm.put("speedFrom", fromSpeed);
                                hm.put("speedBefore", beforeSpeed);
                                hm.put("par", "seconds: "+seconds+ " meters: "+meters);
                                hm.put("seconds",seconds);
                                hm.put("meters", meters);
                                mList.add(hm);
                                conversionListGson.saveSpeedList(KEY_SPEED, pref, mList);
                                adapter.notifyDataSetChanged();
                                Snackbar.make(getCurrentFocus(),  "Изменения сохранены", Snackbar.LENGTH_SHORT)
                                        .setAction("Action", null).show();
                            }
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
                this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
                return null;
            default:
                return null;
        }
    }
}